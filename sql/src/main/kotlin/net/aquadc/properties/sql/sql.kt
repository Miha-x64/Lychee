package net.aquadc.properties.sql

import net.aquadc.persistence.New
import net.aquadc.persistence.struct.BaseStruct
import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.FieldSet
import net.aquadc.persistence.struct.Lens
import net.aquadc.persistence.struct.NamedLens
import net.aquadc.persistence.struct.PartialStruct
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.struct.forEach
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.long
import net.aquadc.persistence.type.nullable
import net.aquadc.properties.Property
import net.aquadc.properties.TransactionalProperty
import net.aquadc.properties.bind
import net.aquadc.properties.internal.ManagedProperty
import net.aquadc.properties.internal.Manager
import net.aquadc.properties.internal.Unset
import net.aquadc.properties.internal.mapToArray
import net.aquadc.properties.persistence.PropertyStruct
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


typealias IdBound = Any // Serializable in some frameworks

typealias SqlProperty<T> = TransactionalProperty<Transaction, T>

interface Session {

    operator fun <SCH : Schema<SCH>, ID : IdBound, REC : Record<SCH, ID>> get(
            table: Table<SCH, ID, REC>
    ): Dao<SCH, ID, REC>

    fun beginTransaction(): Transaction

}

/**
 * Represents a database session specialized for a certain [Table].
 * {@implNote [Manager] supertype is used by [ManagedProperty] instances}
 */
interface Dao<SCH : Schema<SCH>, ID : IdBound, REC : Record<SCH, ID>> : Manager<SCH, Transaction, ID> {
    fun find(id: ID /* TODO fields to prefetch */): REC?
    fun select(condition: WhereCondition<out SCH>, order: Array<out Order<SCH>>/* TODO: prefetch */): Property<List<REC>> // TODO DiffProperty | group by | having
    // TODO: selectWhole(...): Property<List<Property<StructSnapshot<SCH>>>>
    // TODO: fetch(...): List<StructSnapshot<SCH>>
    // todo raw queries, joins
    fun count(condition: WhereCondition<out SCH>): Property<Long>
    // why do they have 'out' variance? Because we want to use a single WhereCondition<Nothing> when there's no condition
}

/**
 * Calls [block] within transaction passing [Transaction] which has functionality to create, mutate, remove [Record]s.
 * In future will retry conflicting transaction by calling [block] more than once.
 */
@UseExperimental(ExperimentalContracts::class)
inline fun <R> Session.withTransaction(block: Transaction.() -> R): R {
    contract {
        callsInPlace(block, InvocationKind.AT_LEAST_ONCE)
    }

    val transaction = beginTransaction()
    try {
        val r = block(transaction)
        transaction.setSuccessful()
        return r
    } finally {
        transaction.close()
    }
}

fun <SCH : Schema<SCH>, ID : IdBound, REC : Record<SCH, ID>> Dao<SCH, ID, REC>.require(id: ID): REC =
        find(id) ?: throw NoSuchElementException("No record found in `$this` for ID $id")

@Suppress("NOTHING_TO_INLINE")
inline fun <SCH : Schema<SCH>, ID : IdBound, REC : Record<SCH, ID>> Dao<SCH, ID, REC>.select(
        condition: WhereCondition<out SCH>, vararg order: Order<SCH>/* TODO: prefetch */
): Property<List<REC>> =
        select(condition, order)

fun <SCH : Schema<SCH>, ID : IdBound, REC : Record<SCH, ID>> Dao<SCH, ID, REC>.selectAll(vararg order: Order<SCH>): Property<List<REC>> =
        select(WhereCondition.Empty, order)

fun Dao<*, *, *>.count(): Property<Long> =
        count(WhereCondition.Empty)

@JvmField val NoOrder = emptyArray<Order<Nothing>>()


interface Transaction : AutoCloseable {

    /**
     * Insert [data] into a [table].
     */
    fun <REC : Record<SCH, ID>, SCH : Schema<SCH>, ID : IdBound> insert(table: Table<SCH, ID, REC>, data: Struct<SCH>): REC

    @Deprecated("this cannot be done safely, with respect to mutability", ReplaceWith("insert(table, data)"))
    fun <REC : Record<SCH, ID>, SCH : Schema<SCH>, ID : IdBound> replace(table: Table<SCH, ID, REC>, data: Struct<SCH>): REC =
            insert(table, data)

    fun <SCH : Schema<SCH>, ID : IdBound, T> update(table: Table<SCH, ID, *>, id: ID, column: FieldDef.Mutable<SCH, T>, columnName: String, value: T)

    fun <SCH : Schema<SCH>, ID : IdBound> delete(record: Record<SCH, ID>)

    /**
     * Clear the whole table.
     * This may be implemented either as `DELETE FROM table` or `TRUNCATE table`.
     */
    fun truncate(table: Table<*, *, *>)

    fun setSuccessful()

    operator fun <REC : Record<SCH, ID>, SCH : Schema<SCH>, ID : IdBound, T> REC.set(field: FieldDef.Mutable<SCH, T>, new: T) {
        (this prop field).setValue(this@Transaction, new)
    }

    /**
     * Updates all [fields] with values from [source].
     */
    fun <REC : Record<SCH, ID>, SCH : Schema<SCH>, ID : IdBound, T> REC.setFrom(
            source: Struct<SCH>, fields: FieldSet<SCH, FieldDef.Mutable<SCH, *>>
    ) {
        source.schema.forEach(fields) {
            mutateFrom(source, it) // capture type
        }
    }
    private inline fun <REC : Record<SCH, ID>, SCH : Schema<SCH>, ID : IdBound, T> REC.mutateFrom(source: Struct<SCH>, field: FieldDef.Mutable<SCH, T>) {
        this[field] = source[field]
    }

}

class Order<SCH : Schema<SCH>>(
        @JvmField internal val col: FieldDef<SCH, *>,
        @JvmField internal val desc: Boolean
) {
    // may become an inline-class when hashCode/equals will be allowed

    override fun hashCode(): Int = // yep, orders on different structs may have interfering hashes
            (if (desc) 0x100 else 0) or col.ordinal.toInt()

    override fun equals(other: Any?): Boolean =
            other === this ||
                    (other is Order<*> && other.col === col && other.desc == desc)

}

val <SCH : Schema<SCH>> FieldDef<SCH, *>.asc: Order<SCH>
    get() = Order(this, false)

val <SCH : Schema<SCH>> FieldDef<SCH, *>.desc: Order<SCH>
    get() = Order(this, true)


/**
 * Represents a table, i. e. defines structs which can be persisted in a database.
 * @param SCH self, i. e. this table
 * @param ID  primary key type
 * @param REC type of record, which can be simply `Record<SCH>` or a custom class extending [Record]
 */
abstract class Table<SCH : Schema<SCH>, ID : IdBound, REC : Record<SCH, ID>>
private constructor(
        val schema: SCH,
        val name: String,
        val idColName: String,
        val idColType: DataType.Simple<ID>,
        val pkField: FieldDef.Immutable<SCH, ID>?
// TODO: [unique] indices
// TODO: a way to declare embedded structs, foreign & join columns
// TODO: maybe a way to declare an immutable field as a primary key
) {

    @Deprecated("this constructor uses Javanese order for id col — 'type name', use Kotlinese 'name type'",
            ReplaceWith("Table(schema, name, idColName, idColType)"))
    constructor(schema: SCH, name: String, idColType: DataType.Simple<ID>, idColName: String) :
            this(schema, name, idColName, idColType, null)

    constructor(schema: SCH, name: String, idColName: String, idColType: DataType.Simple<ID>) :
            this(schema, name, idColName, idColType, null)

    constructor(schema: SCH, name: String, idCol: FieldDef.Immutable<SCH, ID>) :
            this(schema, name, idCol.name, idCol.type as? DataType.Simple<ID>
                    ?: throw IllegalArgumentException("PK column must have simple type"),
                    idCol)

    /**
     * Instantiates a record. Typically consists of a single constructor call.
     */
    abstract fun newRecord(session: Session, primaryKey: ID): REC

    init {
        check(pkField != null || schema.fields.all { idColName != it.name }) {
            "duplicate column: `$name`.`$idColName`"
        }
    }

    /**
     * Returns a list of all relations for this table.
     * This must describe how to store all [Struct] columns relationally.
     */
    protected open fun relations(): List<Relation<SCH, ID, *>> = emptyList()

    private var _fetches: Map<Lens<SCH, REC, *>, FetchStrategy>? = null
    private val _columns: Lazy<ArrayList<NamedLens<SCH, REC, *>>> = lazy {
        val rels = relations().let { rels ->
            rels.associateByTo(New.map<Lens<SCH, REC, *>, Relation<SCH, ID, *>>(rels.size), Relation<SCH, ID, *>::path)
        }
        val columns = ArrayList<NamedLens<SCH, REC, *>>(/* at least */ rels.size + 1)
        if (pkField == null) {
            columns.add(PkLens(this))
        }
        val fetches = New.map<Lens<SCH, REC, *>, FetchStrategy>()
        embed(rels, schema, null, null, false, columns, fetches)

        if (rels.isNotEmpty()) throw RuntimeException("cannot consume relations: $rels")

        this._fetches = fetches
        columns
    }

    @Suppress("UPPER_BOUND_VIOLATED") // some bad code with raw types here
    private fun embed(
            rels: MutableMap<Lens<SCH, REC, *>, Relation<SCH, ID, *>>, schema: Schema<*>,
            naming: NamingConvention?, prefix: NamedLens<SCH, REC, PartialStruct<Schema<*>>?>?, nullize: Boolean,
            outColumns: ArrayList<NamedLens<SCH, REC, *>>, outFetches: MutableMap<Lens<SCH, REC, *>, FetchStrategy>
    ) {
        schema.fields.forEach { field ->
            val path: NamedLens<SCH, REC, out Any?> =
                    if (prefix == null/* implies naming == null*/) field as FieldDef<SCH, *>
                    else /* implies naming != null */ naming!!.concatErased(prefix, field) as NamedLens<SCH, REC, out Any?>

            val type = field.type
            val relSchema = if (type is DataType.Partial<*, *>) {
                type.schema
            } else if (type is DataType.Nullable<*>) {
                val actualType = type.actualType
                if (actualType is DataType.Partial<*, *>) actualType.schema else null
            } else {
                null
            }

            if (relSchema != null) {
                // got a struct type, a relation must be declared
                val rel = rels.remove(path)
                        ?: throw NoSuchElementException("a Relation must be declared for table $name, path $path")

                when (rel) {
                    is Relation.Embedded<*, *, *, *> -> {
                        check(outFetches.put(path, FetchEmbedded) === null)

                        if (rel.fieldSetColName != null) {
                            outColumns.add(SyntheticColLens<SCH, Record<SCH, ID>, Schema<*>, PartialStruct<Schema<*>>?>(
                                    this, rel.fieldSetColName, path as Lens<SCH, Record<SCH, ID>, PartialStruct<Schema<*>>?>, nullize
                            ))
                        }
                        embed(rels, relSchema, rel.naming,
                                path as NamedLens<SCH, REC, PartialStruct<Schema<*>>?>? /* assert it has struct type */,
                                nullize || path.type !is Schema<*>, // if type is nullable or partial, all columns must be nullable
                                outColumns, outFetches
                        )
                    }
                    is Relation.ToOne<*, *, *, *, *> -> TODO()
                    is Relation.ToMany<*, *, *, *, *, *, *> -> TODO()
                    is Relation.ManyToMany<*, *, *, *, *> -> TODO()
                }.also { }
            } else {
                outColumns.add(path)
            }
        }
    }

    val columns: List<NamedLens<SCH, REC, *>>
        get() = _columns.value

    internal fun fetchStrategyFor(lens: Lens<SCH, REC, *>): FetchStrategy {
        val fetches = _fetches ?: _columns.value.let { _ -> _fetches!! /* unwrap lazy */ }
        return fetches[lens] ?: FetchPrimitive
    }

    override fun toString(): String =
            "Table(schema=$schema, name=$name, ${columns.size} columns)"

}

// used internally in some places, don't re-instantiate
internal val nullableLong = nullable(long)

/**
 * The simplest case of [Table] which stores [Record] instances, not ones of its subclasses.
 */
open class SimpleTable<SCH : Schema<SCH>, ID : IdBound> : Table<SCH, ID, Record<SCH, ID>> {

    @Deprecated("this constructor uses Javanese order for id col — 'type name', use Kotlinese 'name type'",
            ReplaceWith("SimpleTable(schema, name, idColName, idColType)"))
    constructor(schema: SCH, name: String, idColType: DataType.Simple<ID>, idColName: String) : super(schema, name, idColName, idColType)

    constructor(schema: SCH, name: String, idColName: String, idColType: DataType.Simple<ID>) : super(schema, name, idColName, idColType)

    constructor(schema: SCH, name: String, idCol: FieldDef.Immutable<SCH, ID>) : super(schema, name, idCol)

    override fun newRecord(session: Session, primaryKey: ID): Record<SCH, ID> =
            Record(this, session, primaryKey)

}


/**
 * Represents an active record — a container with some properties.
 * Subclass it to provide your own getters and/or computed/foreign properties.
 * TODO: should I provide subclassing-less API, too?
 */
open class Record<SCH : Schema<SCH>, ID : IdBound> : BaseStruct<SCH>, PropertyStruct<SCH> {

    internal val table: Table<SCH, ID, *>
    protected val session: Session
    internal val _session get() = session
    val primaryKey: ID

    @Suppress("UNCHECKED_CAST", "UPPER_BOUND_VIOLATED")
    private val dao
        get() = session[table as Table<SCH, ID, Record<SCH, ID>>]

    @JvmField @JvmSynthetic
    internal val values: Array<Any?>  // = ManagedProperty<Transaction, T> | T

    /**
     * Creates new record.
     * Note that such a record is managed and alive (will receive updates) only if created by [Dao].
     */
    constructor(table: Table<SCH, ID, *>, session: Session, primaryKey: ID) : super(table.schema) {
        this.table = table
        this.session = session
        this.primaryKey = primaryKey
        this.values = createValues(session, table, primaryKey)
    }

    @Suppress(
            "UNCHECKED_CAST",
            "UPPER_BOUND_VIOLATED" // RLY, I don't want third generic for Record, this adds no type-safety here
    )
    private fun createValues(session: Session, table: Table<SCH, ID, *>, primaryKey: ID): Array<Any?> =
            session[table as Table<SCH, ID, Record<SCH, ID>>].let { dao ->
                table.schema.fields.mapToArray { col ->
                    when (col) {
                        is FieldDef.Mutable -> ManagedProperty(dao, col as FieldDef.Mutable<SCH, Any?>, /* todo: */ col.name, primaryKey, Unset)
                        is FieldDef.Immutable -> Unset
                    }
                }
            }

    override fun <T> get(field: FieldDef<SCH, T>): T = when (field) {
        is FieldDef.Mutable -> prop(field).value
        is FieldDef.Immutable -> {
            val index = field.ordinal.toInt()
            val value = values[index]

            if (value === Unset) {
                val freshValue = dao.getClean(field, /*TODO*/ field.name, primaryKey)
                values[index] = freshValue
                freshValue
            } else  value as T
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> prop(field: FieldDef.Mutable<SCH, T>): SqlProperty<T> =
            values[field.ordinal.toInt()] as SqlProperty<T>

    var isManaged: Boolean = true
        @JvmSynthetic internal set

    @Suppress("UNCHECKED_CAST") // id is not nullable, so Record<ForeSCH> won't be, too
    infix fun <ForeSCH : Schema<ForeSCH>, ForeID : IdBound, ForeREC : Record<ForeSCH, ForeID>>
            FieldDef.Mutable<SCH, ForeID>.toOne(foreignTable: Table<ForeSCH, ForeID, ForeREC>): SqlProperty<ForeREC> =
            (this as FieldDef.Mutable<SCH, ForeID?>).toOneNullable(foreignTable) as SqlProperty<ForeREC>

    infix fun <ForeSCH : Schema<ForeSCH>, ForeID : IdBound, ForeREC : Record<ForeSCH, ForeID>>
            FieldDef.Mutable<SCH, ForeID?>.toOneNullable(foreignTable: Table<ForeSCH, ForeID, ForeREC>): SqlProperty<ForeREC?> =
            (this@Record prop this@toOneNullable).bind(
                    { id: ForeID? -> if (id == null) null else session[foreignTable].require(id) },
                    { it: ForeREC? -> it?.primaryKey }
            )

    infix fun <ForeSCH : Schema<ForeSCH>, ForeID : IdBound, ForeREC : Record<ForeSCH, ForeID>>
            FieldDef.Mutable<ForeSCH, ID>.toMany(foreignTable: Table<ForeSCH, ForeID, ForeREC>): Property<List<ForeREC>> =
            session[foreignTable].select(this eq primaryKey)

    // TODO: relations for immutable cols

}


/**
 * Creates a property getter, i. e. a function which returns a property of a pre-set [field] of a given [SCH].
 */
fun <SCH : Schema<SCH>, T> propertyGetterOf(field: FieldDef.Mutable<SCH, T>): (Record<SCH, *>) -> Property<T> =
        { it prop field }


internal inline fun <T, U> forEachOfBoth(left: List<T>, right: List<U>, block: (Int, T, U) -> Unit) {
    val size = left.size
    check(right.size == size)
    for (i in 0 until size) {
        block(i, left[i], right[i])
    }
}
