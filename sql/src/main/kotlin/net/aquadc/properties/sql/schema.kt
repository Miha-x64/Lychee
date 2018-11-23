package net.aquadc.properties.sql

import net.aquadc.persistence.struct.BaseStruct
import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.type.DataType
import net.aquadc.properties.Property
import net.aquadc.properties.TransactionalProperty
import net.aquadc.properties.bind
import net.aquadc.properties.internal.ManagedProperty
import net.aquadc.properties.internal.Unset
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


typealias IdBound = Any // Serializable in some frameworks

typealias SqlProperty<T> = TransactionalProperty<Transaction, T>

interface Session {

    operator fun <TBL : Table<TBL, ID, REC>, ID : IdBound, REC : Record<TBL, ID>> get(
            table: Table<TBL, ID, REC>
    ): Dao<TBL, ID, REC>

    fun beginTransaction(): Transaction

}

interface Dao<TBL : Table<TBL, ID, REC>, ID : IdBound, REC : Record<TBL, ID>> {
    fun find(id: ID /* TODO fields to prefetch */): REC?
    fun select(condition: WhereCondition<out TBL>, order: Array<out Order<TBL>>/* TODO: prefetch */): Property<List<REC>> // TODO DiffProperty
    // todo raw queries, joins
    fun count(condition: WhereCondition<out TBL>): Property<Long>
    // why do they have 'out' variance? Because we want to use a single WhereCondition<Nothing> when there's no condition

    // Note: returned [Property] is not managed itself, [Record]s are. fixme may be in LowLevel
    fun <T> createFieldOf(col: MutableCol<TBL, T>, id: ID): ManagedProperty<TBL, Transaction, T>
    fun <T> getValueOf(col: Col<TBL, T>, id: ID): T
}

/**
 * Calls [block] within transaction to create, mutate, remove [Record]s.
 */
@UseExperimental(ExperimentalContracts::class)
inline fun <R> Session.withTransaction(block: Transaction.() -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
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

fun <TBL : Table<TBL, ID, REC>, ID : IdBound, REC : Record<TBL, ID>> Dao<TBL, ID, REC>.require(id: ID): REC =
        find(id) ?: throw IllegalStateException("No record found in `$this` for ID $id")

@Suppress("NOTHING_TO_INLINE")
inline fun <TBL : Table<TBL, ID, REC>, ID : IdBound, REC : Record<TBL, ID>> Dao<TBL, ID, REC>.select(
        condition: WhereCondition<out TBL>, vararg order: Order<TBL>/* TODO: prefetch */
): Property<List<REC>> =
        select(condition, order)

fun <TBL : Table<TBL, ID, REC>, ID : IdBound, REC : Record<TBL, ID>> Dao<TBL, ID, REC>.selectAll(vararg order: Order<TBL>): Property<List<REC>> =
        select(WhereCondition.Empty, order)

fun Dao<*, *, *>.count(): Property<Long> =
        count(WhereCondition.Empty)

@JvmField val NoOrder = emptyArray<Order<Nothing>>()


interface Transaction : AutoCloseable {

    val session: Session

    fun <TBL : Table<TBL, ID, *>, ID : IdBound> insert(table: Table<TBL, ID, *>, vararg contentValues: ColValue<TBL, *>): ID

    fun <TBL : Table<TBL, ID, *>, ID : IdBound, T> update(table: Table<TBL, ID, *>, id: ID, column: MutableCol<TBL, T>, value: T)

    fun <TBL : Table<TBL, ID, *>, ID : IdBound> delete(record: Record<TBL, ID>)

    fun setSuccessful()

    operator fun <REC : Record<TBL, ID>, TBL : Table<TBL, ID, REC>, ID : IdBound, T> REC.set(field: MutableCol<TBL, T>, new: T) {
        (this prop field).setValue(this@Transaction, new)
    }

}

class Order<TBL : Table<TBL, *, *>>(
        @JvmField internal val col: Col<TBL, *>,
        @JvmField internal val desc: Boolean
)

val <TBL : Table<TBL, *, *>> Col<TBL, *>.asc: Order<TBL>
    get() = Order(this, false)

val <TBL : Table<TBL, *, *>> Col<TBL, *>.desc: Order<TBL>
    get() = Order(this, true)


/**
 * Represents a table, i. e. defines structs which can be persisted in a database.
 * @param SCH self, i. e. this table
 * @param ID  primary key type
 * @param REC type of record, which can be simply `Record<SCH>` or a custom class extending [Record]
 * TODO aggregate instead of extending
 */
abstract class Table<SCH : Table<SCH, ID, REC>, ID : IdBound, REC : Record<SCH, ID>>(
        name: String,
        val idColType: DataType<ID>,
        val idColName: String
) : Schema<SCH>(name) {


    abstract fun create(session: Session, id: ID): REC // TODO: rename: instantiate, instantiateRecord, createRecord, newRecord

    final override fun beforeFreeze(nameSet: Set<String>, fields: List<FieldDef<TBL, *>>) {
        check(idColName !in nameSet) { "duplicate column: `$name`.`$idColName`" }
    }

}


typealias Col<TBL, T> = FieldDef<TBL, T>
typealias MutableCol<TBL, T> = FieldDef.Mutable<TBL, T>


/**
 * Represents an active record — a container with some properties.
 * Subclass it to provide your own getters and/or computed/foreign properties.
 * TODO: should I provide subclassing-less API, too?
 */
open class Record<TBL : Table<TBL, ID, *>, ID : IdBound> : BaseStruct<TBL> {

    internal val table: Table<TBL, ID, *>
    internal val session: Session
    val primaryKey: ID

    @Suppress("UPPER_BOUND_VIOLATED") // RLY, I don't want third generic for Record, this adds no type-safety here
    private val dao
        get() = session.get<TBL, ID, Record<TBL, ID>>(table as Table<TBL, ID, Record<TBL, ID>>)

    @JvmField @JvmSynthetic
    internal val values: Array<Any?>  // = ManagedProperty<Transaction, T> | T

    constructor(table: TBL, session: Session, primaryKey: ID) : super(table) {
        this.table = table
        this.session = session
        this.primaryKey = primaryKey
        this.values = @Suppress("UPPER_BOUND_VIOLATED") // RLY, I don't want third generic for Record, this adds no type-safety here
        session.get<TBL, ID, Record<TBL, ID>>(table as Table<TBL, ID, Record<TBL, ID>>).let { dao ->
            table.fields.mapToArray { col ->
                when (col) {
                    is FieldDef.Mutable -> dao.createFieldOf(col as MutableCol<TBL, Nothing>, primaryKey)
                    is FieldDef.Immutable -> Unset
                }
            }
        }
    }

    /*constructor(source: Struct<TBL>) : super(source.type) {
        TODO
    }*/


    @Suppress("UNCHECKED_CAST")
    private fun <T> propOf(field: FieldDef.Mutable<TBL, T>): SqlProperty<T> =
            values[field.ordinal.toInt()] as SqlProperty<T>

    override fun <T> get(field: FieldDef<TBL, T>): T = when (field) {
        is FieldDef.Mutable -> propOf(field).value
        is FieldDef.Immutable -> {
            val index = field.ordinal.toInt()
            val value = values[index]

            if (value === Unset) {
                val freshValue = dao.getValueOf(field, primaryKey)
                values[index] = freshValue
                freshValue
            } else  value as T
        }
    }

    infix fun <T> prop(col: MutableCol<TBL, T>): SqlProperty<T> =
            propOf(col)

    var isManaged: Boolean = true
        @JvmSynthetic internal set

    @Suppress("UNCHECKED_CAST") // id is not nullable, so Record<ForeTBL> won't be, too
    infix fun <ForeTBL : Table<ForeTBL, ForeID, ForeREC>, ForeID : IdBound, ForeREC : Record<ForeTBL, ForeID>>
            MutableCol<TBL, ForeID>.toOne(foreignTable: Table<ForeTBL, ForeID, ForeREC>): SqlProperty<ForeREC> =
            (this as MutableCol<TBL, ForeID?>).toOneNullable(foreignTable) as SqlProperty<ForeREC>

    infix fun <ForeTBL : Table<ForeTBL, ForeID, ForeREC>, ForeID : IdBound, ForeREC : Record<ForeTBL, ForeID>>
            MutableCol<TBL, ForeID?>.toOneNullable(foreignTable: Table<ForeTBL, ForeID, ForeREC>): SqlProperty<ForeREC?> =
            (this@Record prop this@toOneNullable).bind(
                    { id: ForeID? -> if (id == null) null else session[foreignTable].require(id) },
                    { it: ForeREC? -> it?.primaryKey }
            )

    infix fun <ForeTBL : Table<ForeTBL, ForeID, ForeREC>, ForeID : IdBound, ForeREC : Record<ForeTBL, ForeID>>
            MutableCol<ForeTBL, ID>.toMany(foreignTable: Table<ForeTBL, ForeID, ForeREC>): Property<List<ForeREC>> =
            session[foreignTable].select(this eq primaryKey)

    // TODO: relations for immutable cols

}


class ColValue<TBL : Table<TBL, *, *>, T>(val col: Col<TBL, T>, val value: T)

/**
 * Creates a type-safe mapping from a column to its value.
 */
@Suppress("NOTHING_TO_INLINE")
inline operator fun <TBL : Table<TBL, *, *>, T> Col<TBL, T>.minus(value: T): ColValue<TBL, T> =
        ColValue(this, value)


/**
 * Creates a property getter, i. e. a function which returns a property of a pre-set [field] of a given [TBL].
 */
fun <TBL : Table<TBL, *, *>, T> propertyGetterOf(field: MutableCol<TBL, T>): (Record<TBL, *>) -> Property<T> =
        { it prop field }


internal inline fun <T, reified R> List<T>.mapToArray(transform: (T) -> R): Array<R> {
    val array = arrayOfNulls<R>(size)
    for (i in indices) {
        array[i] = transform(this[i])
    }
    @Suppress("UNCHECKED_CAST") // now it's filled with items and not thus not nullable
    return array as Array<R>
}
