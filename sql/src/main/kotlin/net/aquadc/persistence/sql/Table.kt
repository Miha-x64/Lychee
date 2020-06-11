@file:JvmName("Tables")
package net.aquadc.persistence.sql

import net.aquadc.persistence.array
import net.aquadc.persistence.newMap
import net.aquadc.persistence.newSet
import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.ImmutableField
import net.aquadc.persistence.struct.Lens
import net.aquadc.persistence.struct.Named
import net.aquadc.persistence.struct.NamedLens
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.StoredLens
import net.aquadc.persistence.struct.StoredNamedLens
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.struct.forEachIndexed
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.Ilk
import net.aquadc.persistence.type.nothing
import net.aquadc.properties.internal.ManagedProperty
import net.aquadc.properties.internal.Unset


/**
 * Since a [Table] could have type overrides,
 * column type name is either a normal [DataType]<T>,
 * or a [Pair] of type and overriding [CharSequence].
 */
typealias SqlTypeName = Any /*= DataType | CharSequence */

/**
 * Represents a table, i. e. defines structs which can be persisted in a database.
 * @param SCH self, i. e. this table
 * @param ID  primary key type
 */
open class Table<SCH : Schema<SCH>, ID : IdBound>
private constructor(
    val schema: SCH,
    val name: String,
    val idColName: CharSequence,
    idColType: DataType.NotNull.Simple<ID>,
    val pkField: ImmutableField<SCH, ID, out DataType.NotNull.Simple<ID>>? // todo: consistent names, ID || PK
// TODO: [unique] indices https://github.com/greenrobot/greenDAO/blob/72cad8c9d5bf25d6ed3bdad493cee0aee5af8a70/greendao-api/src/main/java/org/greenrobot/greendao/annotation/Index.java
) {

    constructor(schema: SCH, name: String, idColName: String, idColType: DataType.NotNull.Simple<ID>) :
            this(schema, name, idColName, idColType, null)

    constructor(schema: SCH, name: String, idCol: ImmutableField<SCH, ID, out DataType.NotNull.Simple<ID>>) :
            this(schema, name, schema.run { idCol.name }, schema.run { idCol.type }, idCol)

    /**
     * Returns all relations for this table.
     * This must describe how to store all [Struct] columns relationally.
     */
    @Deprecated("now override meta() for relations, indices, type overrides, etc", level = DeprecationLevel.ERROR)
    protected open fun relations(): Array<out ColMeta<SCH>> = noMeta as Array<out ColMeta<SCH>>

    /**
     * Returns all metadata for this table: relations, indices, type overrides.
     * This helps building a list of columns, binding and reading values, constructing 'CREATE TABLE etc.
     * Relations could form dependency circles, that's why we don't require them in constructor.
     * This method could mention other tables but must not touch their columns.
     */
    protected open fun SCH.meta(): Array<out ColMeta<SCH>> = noMeta as Array<out ColMeta<SCH>>

    @JvmSynthetic @JvmField internal var _delegates: Map<StoredLens<SCH, *, *>, SqlPropertyDelegate<SCH, ID>>? = null
    @JvmSynthetic @JvmField internal var _recipe: Array<out Nesting>? = null
//    @JvmSynthetic @JvmField internal var _managedColumns: Array<out StoredNamedLens<SCH, *, *>>? = null
    @JvmSynthetic @JvmField internal var _managedColNames: Array<out CharSequence>? = null
    @JvmSynthetic @JvmField internal var _managedColTypes: Array<out Ilk<*, *>>? = null
    @JvmSynthetic @JvmField internal var _managedColTypeNames: Array<out SqlTypeName>? = null
    @JvmSynthetic @JvmField internal var _idColType: Ilk<ID, DataType.NotNull.Simple<ID>> = idColType // can be changed (overridden) during init
    @JvmSynthetic @JvmField internal var _idColTypeName: SqlTypeName = idColType // can be overridden during init
    private val _columns: Lazy<Array<out StoredNamedLens<SCH, *, *>>> = lazy {
        var relations: MutableMap<StoredLens<SCH, *, *>, ColMeta.Rel<SCH>>? = null
        var types: MutableMap<StoredLens<SCH, *, *>, ColMeta.Type<SCH, *>>? = null
        schema.meta().forEach {
            when (it) {
                is ColMeta.Type<SCH, *> -> check(
                    (types ?: newMap<StoredLens<SCH, *, *>, ColMeta.Type<SCH, *>>().also { types = it })
                        .put(it.path, it) == null)
                is ColMeta.Rel<SCH> -> check(
                    (relations ?: newMap<StoredLens<SCH, *, *>, ColMeta.Rel<SCH>>().also { relations = it })
                        .put(it.path, it) == null)
            }!!
        }
        val columns = CheckNamesList<StoredNamedLens<SCH, *, *>>(schema.fields.size)
        val columnTypes = ArrayList<Ilk<*, *>>(schema.fields.size)
        val columnTypeNames = ArrayList<SqlTypeName>(schema.fields.size)
        if (pkField == null) {
            val pkLens = PkLens(this, _idColType as DataType.NotNull.Simple<ID>)
            columns.add(pkLens, idColName)
            types?.remove(pkLens)?.let(::overrideIdType)
            columnTypeNames.add(_idColTypeName)
            columnTypes.add(_idColType)
        }
        val delegates = newMap<StoredLens<SCH, *, *>, SqlPropertyDelegate<SCH, ID>>()
        val recipe = ArrayList<Nesting>()
        val ss = Nesting.StructStart(false, null, null, schema)
        recipe.add(ss)
        embed(relations, types, schema, null, null, columns, columnTypes, columnTypeNames, delegates, recipe)
        ss.colCount = columns.size
        recipe.add(Nesting.StructEnd)
        this._recipe = recipe.array()

        relations?.takeIf { it.isNotEmpty() }?.let { throw RuntimeException("Cannot consume relations: ${it.values}") }

        this._delegates = delegates//todo:.optimizeReadOnlyMap()

        val colsArray = columns.array()
        val skipPkCol = pkField == null
        val managedColumns = if (skipPkCol) columns.subList(1, columns.size).array() else colsArray
        val manColTs = if (skipPkCol) columnTypes.subList(1, columns.size) else columnTypes
        _managedColTypes = manColTs.array()
        val manColTNs = if (skipPkCol) columnTypeNames.subList(1, columns.size) else columnTypeNames
        _managedColTypeNames = if (manColTs == manColTNs) _managedColTypes else manColTNs.array()
        _managedColNames = managedColumns.mapIndexedToArray { _, it -> it.name(schema) }

        types?.remove(columns[0]) // we've unconditionally peeked it earlier and conditionally polled within embed()
        types?.takeIf { it.isNotEmpty() }?.let { throw RuntimeException("Cannot consume type overrides: " +
            it.values.map { t -> t.javaClass.simpleName + '(' + t.path + ')' }) }

        colsArray
    }

    @JvmSynthetic internal fun overrideIdType(type: ColMeta.Type<SCH, *>) {
        (type.typeName ?: type.override?.custom?.name ?: type.override?.type)?.let { _idColTypeName = it }
        type.override?.let { _idColType = it as Ilk<ID, DataType.NotNull.Simple<ID>> }
    }

    internal class CheckNamesList<E : Named<*>>(initialCapacity: Int) : ArrayList<E>(initialCapacity) {
        private val names = newSet<String>(initialCapacity)
        fun add(element: E, itsName: CharSequence): Boolean {
            check(itsName.isNotBlank()) { "column has blank name: $element" }
            check(names.add(itsName.toString())) { "duplicate name '$itsName'" }
            return super.add(element)
        }
    }

    // some bad code with raw types here
    @Suppress("UPPER_BOUND_VIOLATED") @JvmSynthetic internal fun embed(
        rels: MutableMap<StoredLens<SCH, *, *>, ColMeta.Rel<SCH>>?,
        types: MutableMap<StoredLens<SCH, *, *>, ColMeta.Type<SCH, *>>?,
        schema: Schema<*>,
        naming: NamingConvention?, prefix: StoredNamedLens<SCH, *, *>?,
        outColumns: CheckNamesList<StoredNamedLens<SCH, *, *>>,
        outColumnTypes: ArrayList<Ilk<*, *>>,
        outColumnTypeNames: ArrayList<SqlTypeName>,
        outDelegates: MutableMap<StoredLens<SCH, *, *>, SqlPropertyDelegate<SCH, ID>>?,
        outRecipe: ArrayList<Nesting>
    ) {
        val fields = schema.fields
        val fieldCount = fields.size
        for (i in 0 until fieldCount) {
            val field = fields[i]
            val path: StoredNamedLens<SCH, out Any?, *> =
                    if (prefix == null/* implies naming == null*/) field as FieldDef<SCH, *, *>
                    else /* implies naming != null */ naming!!.concatErased(this.schema, schema, prefix, field) as StoredNamedLens<SCH, out Any?, out DataType<Any?>>

            val type = (field as FieldDef<Schema<*>, Any?, DataType<Any?>>).type(schema)
            val tOverr = types?.remove(path)
            if (tOverr != null) {
                addColumn(outColumns, outColumnTypes, outColumnTypeNames, path, tOverr)
                if (outColumns.size == 1) overrideIdType(tOverr) // this is primary key
            } else when (type) {
                is DataType.NotNull.Partial<*, *> -> type
                is DataType.Nullable<*, *> -> type.actualType as? DataType.NotNull.Partial<*, *>
                // ignore collections of (partial) structs, they can be stored only within 'real' relations while we support only Embedded ones at the moment
                else -> null
            }?.let { relType ->
                // got a struct type, a relation must be declared
                addRelationalCols(
                    rels, path, relType, outColumns, schema, types,
                    outColumnTypes, outColumnTypeNames, outRecipe, field, type, outDelegates
                )
            } ?: run {
                addColumn(outColumns, outColumnTypes, outColumnTypeNames, path, null)
            }
        }
    }

    @Suppress("UPPER_BOUND_VIOLATED")
    private fun addRelationalCols(
        rels: MutableMap<StoredLens<SCH, *, *>, ColMeta.Rel<SCH>>?,
        path: StoredNamedLens<SCH, out Any?, *>,
        relType: DataType.NotNull.Partial<*, *>,
        outColumns: CheckNamesList<StoredNamedLens<SCH, *, *>>,
        schema: Schema<*>,
        types: MutableMap<StoredLens<SCH, *, *>, ColMeta.Type<SCH, *>>?,
        outColumnTypes: ArrayList<Ilk<*, *>>,
        outColumnTypeNames: ArrayList<SqlTypeName>,
        outRecipe: ArrayList<Nesting>,
        field: FieldDef<out Schema<*>, out Any?, out DataType<*>>,
        type: DataType<Any?>,
        outDelegates: MutableMap<StoredLens<SCH, *, *>, SqlPropertyDelegate<SCH, ID>>?
    ) {
        val rel = rels?.remove(path)
            ?: throw NoSuchElementException("${this@Table} requires a Relation to be declared for path $path storing values of type $relType")

        when (rel) {
            is ColMeta.Rel.Embed<*> -> {
                val start = outColumns.size
                val fieldSetCol = rel.fieldSetColName?.let { fieldSetColName ->
                    (rel.naming.concatErased(this.schema, schema, path, FieldSetLens<Schema<*>>(fieldSetColName))
                        as StoredNamedLens<SCH, out Long?, *>).also { path ->
                        val tOverr = types?.remove(path)
                        addColumn(outColumns, outColumnTypes, outColumnTypeNames, path, tOverr)
                    }
                }

                val relSchema = relType.schema
                val recipeStart = outRecipe.size
                val ss = Nesting.StructStart(fieldSetCol != null, field, type, relType)
                outRecipe.add(ss)
                embed(
                    rels, types, relSchema, rel.naming, path,
                    outColumns, outColumnTypes, outColumnTypeNames, null, outRecipe
                )
                ss.colCount = outColumns.size - start
                outRecipe.add(Nesting.StructEnd)

                val subColumns = outColumns.subList(start, outColumns.size)
                check(outDelegates?.put(path, Embedded(
                    // ArrayList$SubList checks for modifications and cannot be passed as is
                    outRecipe.subList(recipeStart, outRecipe.size).array(),
                    start,
                    Array(subColumns.size) { i -> subColumns[i].name(this.schema) },
                    outColumnTypes.subList(start, outColumns.size).array()
                )) === null)
            }
            else -> TODO()
//            is Relation.ToOne<*, *, *, *, *> ->
//            is Relation.ToMany<*, *, *, *, *, *, *> ->
//            is Relation.ManyToMany<*, *, *, *, *> ->
        }.also { }
    }

    private fun addColumn(
        outColumns: CheckNamesList<StoredNamedLens<SCH, *, *>>,
        outColumnTypes: ArrayList<Ilk<*, *>>,
        outColumnTypeNames: ArrayList<SqlTypeName>,
        path: StoredNamedLens<SCH, *, *>,
        tOverr: ColMeta.Type<SCH, *>?
    ) {
        val t = path.type(this.schema) as Ilk<*, *>
        outColumns.add(path, path.name(schema))
        outColumnTypes.add(tOverr?.override ?: t)
        outColumnTypeNames.add(tOverr?.typeName ?: tOverr?.override?.custom?.name ?: tOverr?.override?.type ?: t)
    }

    internal sealed class Nesting {
        class StructStart constructor(
                @JvmField val hasFieldSet: Boolean,
                @JvmField val myField: FieldDef<*, *, *>?,
                @JvmField val type: DataType<*>?,
                @JvmField val unwrappedType: DataType.NotNull.Partial<*, *>
        ) : Nesting() {
            @JvmField var colCount: Int = 0
        }

        object StructEnd : Nesting() // todo kill me
    }

    val columns: Array<out StoredNamedLens<SCH, *, *>>
        get() = _columns.value

    val idColType: Ilk<ID, DataType.NotNull.Simple<ID>>
        get() = columns.let { _idColType } // not ?: 'cause it can be assigned but require override

    val idColTypeName: SqlTypeName //= DataType.Simple<ID> | CharSequence
        get() = columns.let { _idColTypeName } // not ?: 'cause it can be assigned but require override

    val pkColumn: NamedLens<SCH, Record<SCH, ID>, Record<SCH, ID>, ID, out DataType.NotNull.Simple<ID>>
        get() = pkField ?: _columns.let {
            if (it.isInitialized())
                it.value[0] as NamedLens<SCH, Record<SCH, ID>, Record<SCH, ID>, ID, out DataType.NotNull.Simple<ID>>
            else PkLens(this, _idColType.type as DataType.NotNull.Simple<ID>) as NamedLens<SCH, Record<SCH, ID>, Record<SCH, ID>, ID, out DataType.NotNull.Simple<ID>>
        }

    internal val recipe: Array<out Nesting>
        get() = _recipe ?: _columns.value.let { _ /* unwrap lazy */ -> _recipe!! }

    fun indexOfManaged(column: StoredLens<SCH, *, *>): Int {
        val idxOfCol = columns.indexOf(column)
        // _managedColumns = if (pkField == null) columns.subList(1, columns.size).array() else colsArray
        // [unmanaged PK (0, -1 for us), 1 (0 for us), 2 (1 for us), …]
        return idxOfCol - (if (pkField == null && idxOfCol >= 0) 1 else 0)
    }

    /*val managedColumns: Array<out StoredNamedLens<SCH, *, *>>
        get() = _managedColumns ?: _columns.value.let { _ /* unwrap lazy */ -> _managedColumns!! }*/

    val managedColNames: Array<out CharSequence>
        get() = _managedColNames ?: _columns.value.let { _managedColNames!! }

    val managedColTypes: Array<out Ilk<*, *>>
        get() = _managedColTypes ?: _columns.value.let { _managedColTypes!! }

    val managedColTypeNames: Array<out SqlTypeName>
        get() = _managedColTypeNames ?: _columns.value.let { _managedColTypeNames!! }

    private var _columnsByName: Map<String, StoredNamedLens<SCH, *, *>>? = null

    @Deprecated("names are now `CharSequence`s with undefined hashCode()/equals()")
    val columnsByName: Nothing
        get() = throw AssertionError()


    private var _columnIndices: Map<StoredNamedLens<SCH, *, *>, Int>? = null

    val columnIndices: Map<StoredNamedLens<SCH, *, *>, Int>
        get() = _columnIndices
                ?: columns.let { cols ->
                    newMap<StoredNamedLens<SCH, *, *>, Int>(cols.size).also { map ->
                        columns.forEachIndexed { i, col -> map[col] = i }
                    }
                }.also { _columnIndices = it }

    internal fun delegateFor(lens: Lens<SCH, Record<SCH, ID>, Record<SCH, ID>, *, *>): SqlPropertyDelegate<SCH, ID> {
        val delegates = _delegates ?: _columns.value.let { _ /* unwrap lazy */ -> _delegates!! }
        return delegates[lens] ?: simpleDelegate as SqlPropertyDelegate<SCH, ID>
    }
    internal fun <T> columnByLens(lens: StoredLens<SCH, T, *>): StoredNamedLens<SCH, T, *>? =
            colIndexByLens(lens)?.let { columns[it] as StoredNamedLens<SCH, T, *> }

    fun <T, DT : DataType<T>> typeOf(col: StoredLens<SCH, T, *>): Ilk<T, *> = (
        if (col == pkColumn) idColType else managedColTypes[colIndexByLens(col)!! - columns.size + managedColNames.size]
    ) as Ilk<T, *>

    private fun <T> colIndexByLens(lens: StoredLens<SCH, T, *>) =
        (columnIndices as Map<StoredLens<SCH, *, *>, Int>)[lens]

    @JvmSynthetic internal fun commitValues(record: Record<SCH, ID>, mutFieldValues: Array<Any?>) {
        schema.forEachIndexed(schema.mutableFieldSet) { i, field ->
            val value = mutFieldValues[i]
            if (value !== Unset) {
                (record.values[field.ordinal.toInt()] as ManagedProperty<SCH, *, Any?, ID>).commit(value)
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Table<*, *> || other.name != name) return false
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

        // don't trigger initialization, it could be broken
        if (_columns.isInitialized()) append(columns.size).append(" columns")
        else append("uninitialized")

        append(')')
    }

    private companion object {
        private val simpleDelegate = Simple<Nothing, Nothing>()
        private val noMeta = emptyArray<ColMeta<Nothing>>()
    }

}

/**
 * The simplest case of [Table] which stores [Record] instances, not ones of its subclasses.
 */
@Deprecated("normal Table is Simple enough", ReplaceWith("Table"), DeprecationLevel.ERROR)
typealias SimpleTable<SCH, ID> = Table<SCH, ID>

@Suppress("NOTHING_TO_INLINE") // pass-through
inline fun <SCH : Schema<SCH>, ID : IdBound> tableOf(schema: SCH, name: String, idColName: String, idColType: DataType.NotNull.Simple<ID>): Table<SCH, ID> =
        Table(schema, name, idColName, idColType)

@Suppress("NOTHING_TO_INLINE") // pass-through
inline fun <SCH : Schema<SCH>, ID : IdBound> tableOf(schema: SCH, name: String, idCol: ImmutableField<SCH, ID, out DataType.NotNull.Simple<ID>>): Table<SCH, ID> =
        Table(schema, name, idCol)

// just extend Table,
// but infer type arguments instead of forcing client to specify them explicitly in object expression
// (he-he, modern Java allows writing `new Table<>() {}`):

inline fun <SCH : Schema<SCH>, ID : IdBound> tableOf(
    schema: SCH, name: String, idColName: String, idColType: DataType.NotNull.Simple<ID>,
    crossinline meta: SCH.() -> Array<out ColMeta<SCH>>
): Table<SCH, ID> =
        object : Table<SCH, ID>(schema, name, idColName, idColType) {
            override fun SCH.meta(): Array<out ColMeta<SCH>> = meta.invoke(schema)
        }

inline fun <SCH : Schema<SCH>, ID : IdBound> tableOf(
    schema: SCH, name: String, idCol: ImmutableField<SCH, ID, out DataType.NotNull.Simple<ID>>,
    crossinline meta: SCH.() -> Array<out ColMeta<SCH>>
): Table<SCH, ID> =
        object : Table<SCH, ID>(schema, name, idCol) {
            override fun SCH.meta(): Array<out ColMeta<SCH>> = meta.invoke(schema)
        }

@Suppress("NOTHING_TO_INLINE") // just to be consistent with other functions
inline fun <SCH : Schema<SCH>> projection(schema: SCH): Table<SCH, Nothing> =
        tableOf(schema, "<anonymous>", "<none>", nothing)

inline fun <SCH : Schema<SCH>> projection(schema: SCH, crossinline relations: SCH.() -> Array<out ColMeta<SCH>>): Table<SCH, Nothing> =
        tableOf(schema, "<anonymous>", "<none>", nothing, relations)
