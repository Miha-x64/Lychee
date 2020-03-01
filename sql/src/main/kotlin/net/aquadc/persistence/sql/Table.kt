package net.aquadc.persistence.sql

import net.aquadc.persistence.New
import net.aquadc.persistence.array
import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.Lens
import net.aquadc.persistence.struct.Named
import net.aquadc.persistence.struct.NamedLens
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.StoredLens
import net.aquadc.persistence.struct.StoredNamedLens
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.nothing
import net.aquadc.properties.internal.ManagedProperty
import net.aquadc.properties.internal.Unset


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
        val pkField: FieldDef.Immutable<SCH, ID, out DataType.Simple<ID>>? // todo: consistent names, ID || PK
// TODO: [unique] indices https://github.com/greenrobot/greenDAO/blob/72cad8c9d5bf25d6ed3bdad493cee0aee5af8a70/greendao-api/src/main/java/org/greenrobot/greendao/annotation/Index.java
// TODO: auto increment
) {

    constructor(schema: SCH, name: String, idColName: String, idColType: DataType.Simple<ID>) :
            this(schema, name, idColName, idColType, null)

    constructor(schema: SCH, name: String, idCol: FieldDef.Immutable<SCH, ID, out DataType.Simple<ID>>) :
            this(schema, name, idCol.name, idCol.type, idCol)

    /**
     * Instantiates a record. Typically consists of a single constructor call.
     */
    @Deprecated("Stop overriding this! Will become final.")
    abstract fun newRecord(session: Session<*>, primaryKey: ID): REC

    /**
     * Returns all relations for this table.
     * This must describe how to store all [Struct] columns relationally.
     */
    protected open fun relations(): Array<out Relation<SCH, ID, *>> = noRelations as Array<Relation<SCH, ID, *>>

    @JvmSynthetic @JvmField internal var _delegates: Map<StoredLens<SCH, *, *>, SqlPropertyDelegate<SCH, ID>>? = null
    @JvmSynthetic @JvmField internal var _recipe: Array<out Nesting>? = null
    @JvmSynthetic @JvmField internal var _columnsMappedToFields: Array<out StoredNamedLens<SCH, *, *>>? = null
    private val _columns: Lazy<Array<out StoredNamedLens<SCH, *, *>>> = lazy {
        val rels = relations().let { rels ->
            rels.associateByTo(New.map<StoredLens<SCH, *, *>, Relation<SCH, ID, *>>(rels.size), Relation<SCH, ID, *>::path)
        }
        val columns = CheckNamesList<StoredNamedLens<SCH, *, *>>(schema.fields.size)
        if (pkField == null) {
            columns.add(PkLens(this))
        }
        val delegates = New.map<StoredLens<SCH, *, *>, SqlPropertyDelegate<SCH, ID>>()
        val recipe = ArrayList<Nesting>()
        val ss = Nesting.StructStart(false, null, schema)
        recipe.add(ss)
        embed(rels, schema, null, null, columns, delegates, recipe)
        ss.colCount = columns.size
        recipe.add(Nesting.StructEnd)
        this._recipe = recipe.array()

        if (rels.isNotEmpty()) throw RuntimeException("cannot consume relations: $rels")

        this._delegates = delegates
        val colsArray = columns.array()
        _columnsMappedToFields = if (pkField == null) columns.subList(1, columns.size).array() else colsArray
        colsArray
    }

    private class CheckNamesList<E : Named>(initialCapacity: Int) : ArrayList<E>(initialCapacity) {
        private val names = New.set<String>(initialCapacity)
        override fun add(element: E): Boolean {
            val name = element.name
            check(name.isNotBlank()) { "column has blank name: $element" }
            check(names.add(name)) { "duplicate name '$name' assigned to both [${first { it.name == name }}, $element]" }
            return super.add(element)
        }
    }

    // some bad code with raw types here
    @Suppress("UPPER_BOUND_VIOLATED") @JvmSynthetic internal fun embed(
            rels: MutableMap<StoredLens<SCH, *, *>, Relation<SCH, ID, *>>, schema: Schema<*>,
            naming: NamingConvention?, prefix: StoredNamedLens<SCH, *, *>?,
            outColumns: ArrayList<StoredNamedLens<SCH, *, *>>,
            outDelegates: MutableMap<StoredLens<SCH, *, *>, SqlPropertyDelegate<SCH, ID>>?,
            outRecipe: ArrayList<Nesting>
    ) {
        val fields = schema.fields
        val fieldCount = fields.size
        for (i in 0 until fieldCount) {
            val field = fields[i]
            val path: StoredNamedLens<SCH, out Any?, *> =
                    if (prefix == null/* implies naming == null*/) field as FieldDef<SCH, *, *>
                    else /* implies naming != null */ naming!!.concatErased(prefix, field) as StoredNamedLens<SCH, out Any?, out DataType<Any?>>

            val relType = when (val type = field.type) {
                is DataType.Partial<*, *> -> type
                is DataType.Nullable<*, *> -> type.actualType as? DataType.Partial<*, *>
                // ignore collections of (partial) structs, the can be stored only within 'real' relations while we support only Embedded ones at the moment
                else -> null
            }

            if (relType != null) {
                // got a struct type, a relation must be declared
                val rel = rels.remove(path)
                        ?: throw NoSuchElementException("${this@Table} requires a Relation to be declared for path $path storing values of type $relType")

                when (rel) {
                    is Relation.Embedded<*, *, *> -> {
                        val start = outColumns.size
                        val fieldSetCol = rel.fieldSetColName?.let { fieldSetColName ->
                            (rel.naming.concatErased(path, FieldSetLens<Schema<*>>(fieldSetColName)) as StoredNamedLens<SCH, out Long?, *>)
                                    .also { outColumns.add(it) }
                        }

                        val relSchema = relType.schema
                        val recipeStart = outRecipe.size
                        val ss = Nesting.StructStart(fieldSetCol != null, field, relType)
                        outRecipe.add(ss)
                        /*val nestedLenses =*/ embed(rels, relSchema, rel.naming, path, outColumns, null, outRecipe)
                        ss.colCount = outColumns.size - start
                        outRecipe.add(Nesting.StructEnd)

                        check(outDelegates?.put(path, Embedded<SCH, ID, Schema<*>>(
                                outColumns.subList(start, outColumns.size).array(),
                                // ArrayList$SubList checks for concurrent modifications and cannot be passed as is
                                outRecipe.subList(recipeStart, outRecipe.size).array()
                        )) === null)
                    }
                    else -> TODO()
//                    is Relation.ToOne<*, *, *, *, *> ->
//                    is Relation.ToMany<*, *, *, *, *, *, *> ->
//                    is Relation.ManyToMany<*, *, *, *, *> ->
                }.also { }
            } else {
                outColumns.add(path)
            }
        }
    }

    internal sealed class Nesting {
        class StructStart constructor(
                @JvmField val hasFieldSet: Boolean,
                @JvmField val myField: FieldDef<*, *, *>?,
                @JvmField val unwrappedType: DataType.Partial<*, *>
        ) : Nesting() {
            @JvmField var colCount: Int = 0
        }

        object StructEnd : Nesting()
    }

    val columns: Array<out StoredNamedLens<SCH, *, *>>
        get() = _columns.value

    val pkColumn: NamedLens<SCH, REC, REC, ID, out DataType.Simple<ID>>
        get() = columns[0] as NamedLens<SCH, REC, REC, ID, out DataType.Simple<ID>>

    internal val recipe: Array<out Nesting>
        get() = _recipe ?: _columns.value.let { _ /* unwrap lazy */ -> _recipe!! }

    val columnsMappedToFields: Array<out StoredNamedLens<SCH, *, *>>
        get() = _columnsMappedToFields ?: _columns.value.let { _ /* unwrap lazy */ -> _columnsMappedToFields!! }


    private var _columnsByName: Map<String, StoredNamedLens<SCH, *, *>>? = null

    val columnsByName: Map<String, StoredNamedLens<SCH, *, *>>
        get() = _columnsByName
                ?: columns.let { cols ->
                    cols.associateByTo(New.map(cols.size), StoredNamedLens<SCH, *, *>::name)
                }.also { _columnsByName = it }


    private var _columnIndices: Map<StoredNamedLens<SCH, *, *>, Int>? = null

    val columnIndices: Map<StoredNamedLens<SCH, *, *>, Int>
        get() = _columnIndices
                ?: columns.let { cols ->
                    New.map<StoredNamedLens<SCH, *, *>, Int>(cols.size).also { map ->
                        columns.forEachIndexed { i, col -> map[col] = i }
                    }
                }.also { _columnIndices = it }

    internal fun delegateFor(lens: Lens<SCH, REC, REC, *, *>): SqlPropertyDelegate<SCH, ID> {
        val delegates = _delegates ?: _columns.value.let { _ /* unwrap lazy */ -> _delegates!! }
        return delegates[lens] ?: simpleDelegate as SqlPropertyDelegate<SCH, ID>
    }
    internal fun <T> columnByLens(lens: StoredLens<SCH, T, *>): StoredNamedLens<SCH, T, *>? =
            (columnIndices as Map<StoredLens<SCH, *, *>, Int>)[lens]?.let { columns[it] as StoredNamedLens<SCH, T, *> }

    @JvmSynthetic internal fun commitValues(record: Record<SCH, ID>, mutFieldValues: Array<Any?>) {
        val mutFields = schema.mutableFields
        for (i in mutFields.indices) {
            val value = mutFieldValues[i]
            if (value !== Unset) {
                (record.values[mutFields[i].ordinal.toInt()] as ManagedProperty<SCH, *, Any?, ID>).commit(value)
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Table<*, *, *> || other.name != name) return false
        require(this === other) { "tables have same name but different instances: '$this' and '$other" }
        return true
    }

    override fun hashCode(): Int =
            name.hashCode()

    override fun toString(): String = buildString {
        append("Table(" +
                "schema=").append(schema)
                .append(", name=").append(name)
                .append(", ")

        // don't trigger initialization, it may be broken
        if (_columns.isInitialized()) append(columns.size).append(" columns")
        else append("uninitialized")

        append(')')
    }

    private companion object {
        private val simpleDelegate = Simple<Nothing, Nothing>()
        private val noRelations = emptyArray<Relation<Nothing, Nothing, Nothing>>()
    }

}

/**
 * The simplest case of [Table] which stores [Record] instances, not ones of its subclasses.
 */
open class SimpleTable<SCH : Schema<SCH>, ID : IdBound> : Table<SCH, ID, Record<SCH, ID>> {

    constructor(schema: SCH, name: String, idColName: String, idColType: DataType.Simple<ID>) : super(schema, name, idColName, idColType)

    constructor(schema: SCH, name: String, idCol: FieldDef.Immutable<SCH, ID, out DataType.Simple<ID>>) : super(schema, name, idCol)

    @Deprecated("Stop overriding this! Will become final.")
    override fun newRecord(session: Session<*>, primaryKey: ID): Record<SCH, ID> =
            Record(this, session, primaryKey)

}

@Suppress("NOTHING_TO_INLINE") // pass-through
inline fun <SCH : Schema<SCH>, ID : IdBound> tableOf(schema: SCH, name: String, idColName: String, idColType: DataType.Simple<ID>): SimpleTable<SCH, ID> =
        SimpleTable(schema, name, idColName, idColType)

@Suppress("NOTHING_TO_INLINE") // pass-through
inline fun <SCH : Schema<SCH>, ID : IdBound> tableOf(schema: SCH, name: String, idCol: FieldDef.Immutable<SCH, ID, out DataType.Simple<ID>>): SimpleTable<SCH, ID> =
        SimpleTable(schema, name, idCol)

// just extend SimpleTable, but infer type arguments instead of forcing client to specify them explicitly in object expression:

inline fun <SCH : Schema<SCH>, ID : IdBound> tableOf(
        schema: SCH, name: String, idColName: String, idColType: DataType.Simple<ID>,
        crossinline relations: SCH.() -> Array<out Relation<SCH, ID, *>>
): SimpleTable<SCH, ID> =
        object : SimpleTable<SCH, ID>(schema, name, idColName, idColType) {
            override fun relations(): Array<out Relation<SCH, ID, *>> = relations.invoke(schema)
        }

inline fun <SCH : Schema<SCH>, ID : IdBound> tableOf(
        schema: SCH, name: String, idCol: FieldDef.Immutable<SCH, ID, out DataType.Simple<ID>>,
        crossinline relations: () -> Array<out Relation<SCH, ID, *>>
): SimpleTable<SCH, ID> =
        object : SimpleTable<SCH, ID>(schema, name, idCol) {
            override fun relations(): Array<out Relation<SCH, ID, *>> = relations.invoke()
        }

@Suppress("NOTHING_TO_INLINE") // just to be consistent with other functions
inline fun <SCH : Schema<SCH>> projection(schema: SCH): SimpleTable<SCH, Nothing> =
        tableOf(schema, "<anonymous>", "<none>", nothing)

inline fun <SCH : Schema<SCH>> projection(schema: SCH, crossinline relations: SCH.() -> Array<out Relation<SCH, Nothing, *>>): SimpleTable<SCH, Nothing> =
        tableOf(schema, "<anonymous>", "<none>", nothing, relations)
