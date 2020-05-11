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
import net.aquadc.persistence.type.nothing
import net.aquadc.properties.internal.ManagedProperty
import net.aquadc.properties.internal.Unset


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
        val idColType: DataType.Simple<ID>,
        val pkField: ImmutableField<SCH, ID, out DataType.Simple<ID>>? // todo: consistent names, ID || PK
// TODO: [unique] indices https://github.com/greenrobot/greenDAO/blob/72cad8c9d5bf25d6ed3bdad493cee0aee5af8a70/greendao-api/src/main/java/org/greenrobot/greendao/annotation/Index.java
// TODO: auto increment
) {

    constructor(schema: SCH, name: String, idColName: String, idColType: DataType.Simple<ID>) :
            this(schema, name, idColName, idColType, null)

    constructor(schema: SCH, name: String, idCol: ImmutableField<SCH, ID, out DataType.Simple<ID>>) :
            this(schema, name, schema.run { idCol.name }, schema.run { idCol.type }, idCol)

    /**
     * Returns all relations for this table.
     * This must describe how to store all [Struct] columns relationally.
     */
    protected open fun relations(): Array<out Relation<SCH, ID, *>> = noRelations as Array<Relation<SCH, ID, *>>

    @JvmSynthetic @JvmField internal var _delegates: Map<StoredLens<SCH, *, *>, SqlPropertyDelegate<SCH, ID>>? = null
    @JvmSynthetic @JvmField internal var _recipe: Array<out Nesting>? = null
    @JvmSynthetic @JvmField internal var _managedColumns: Array<out StoredNamedLens<SCH, *, *>>? = null
    @JvmSynthetic @JvmField internal var _managedColNames: Array<out CharSequence>? = null
    @JvmSynthetic @JvmField internal var _managedColTypes: Array<out DataType<*>>? = null
    private val _columns: Lazy<Array<out StoredNamedLens<SCH, *, *>>> = lazy {
        val rels = relations().let { rels ->
            rels.associateByTo(newMap<StoredLens<SCH, *, *>, Relation<SCH, ID, *>>(rels.size), Relation<SCH, ID, *>::path)
        }
        val columns = CheckNamesList<StoredNamedLens<SCH, *, *>>(schema.fields.size)
        if (pkField == null) {
            columns.add(PkLens(this), idColName)
        }
        val delegates = newMap<StoredLens<SCH, *, *>, SqlPropertyDelegate<SCH, ID>>()
        val recipe = ArrayList<Nesting>()
        val ss = Nesting.StructStart(false, null, null, schema)
        recipe.add(ss)
        embed(rels, schema, null, null, columns, delegates, recipe)
        ss.colCount = columns.size
        recipe.add(Nesting.StructEnd)
        this._recipe = recipe.array()

        if (rels.isNotEmpty()) throw RuntimeException("cannot consume relations: $rels")

        this._delegates = delegates
        val colsArray = columns.array()
        _managedColumns = if (pkField == null) columns.subList(1, columns.size).array() else colsArray
        _managedColNames = _managedColumns!!.mapIndexedToArray { _, it -> it.name(schema) }
        _managedColTypes = _managedColumns!!.mapIndexedToArray { _, it -> it.type(schema) }
        colsArray
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
            rels: MutableMap<StoredLens<SCH, *, *>, Relation<SCH, ID, *>>, schema: Schema<*>,
            naming: NamingConvention?, prefix: StoredNamedLens<SCH, *, *>?,
            outColumns: CheckNamesList<StoredNamedLens<SCH, *, *>>,
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
            val relType = when (type) {
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
                            (rel.naming.concatErased(this.schema, schema, path, FieldSetLens<Schema<*>>(fieldSetColName)) as StoredNamedLens<SCH, out Long?, *>)
                                    .also { outColumns.add(it, it.name(this.schema)) }
                        }

                        val relSchema = relType.schema
                        val recipeStart = outRecipe.size
                        val ss = Nesting.StructStart(fieldSetCol != null, field, type, relType)
                        outRecipe.add(ss)
                        /*val nestedLenses =*/ embed(rels, relSchema, rel.naming, path, outColumns, null, outRecipe)
                        ss.colCount = outColumns.size - start
                        outRecipe.add(Nesting.StructEnd)

                        check(outDelegates?.put(path, Embedded(
                                this.schema,
                                outColumns.subList(start, outColumns.size),
                                // ArrayList$SubList checks for modifications and cannot be passed as is
                                outRecipe.subList(recipeStart, outRecipe.size).array(),
                                start
                        )) === null)
                    }
                    else -> TODO()
//                    is Relation.ToOne<*, *, *, *, *> ->
//                    is Relation.ToMany<*, *, *, *, *, *, *> ->
//                    is Relation.ManyToMany<*, *, *, *, *> ->
                }.also { }
            } else {
                outColumns.add(path, path.name(this.schema))
            }
        }
    }

    internal sealed class Nesting {
        class StructStart constructor(
                @JvmField val hasFieldSet: Boolean,
                @JvmField val myField: FieldDef<*, *, *>?,
                @JvmField val type: DataType<*>?,
                @JvmField val unwrappedType: DataType.Partial<*, *>
        ) : Nesting() {
            @JvmField var colCount: Int = 0
        }

        object StructEnd : Nesting()
    }

    val columns: Array<out StoredNamedLens<SCH, *, *>>
        get() = _columns.value

    val pkColumn: NamedLens<SCH, Record<SCH, ID>, Record<SCH, ID>, ID, out DataType.Simple<ID>>
        get() = columns[0] as NamedLens<SCH, Record<SCH, ID>, Record<SCH, ID>, ID, out DataType.Simple<ID>>

    internal val recipe: Array<out Nesting>
        get() = _recipe ?: _columns.value.let { _ /* unwrap lazy */ -> _recipe!! }

    val managedColumns: Array<out StoredNamedLens<SCH, *, *>>
        get() = _managedColumns ?: _columns.value.let { _ /* unwrap lazy */ -> _managedColumns!! }

    val managedColNames: Array<out CharSequence>
        get() = _managedColNames ?: _columns.value.let { _managedColNames!! }

    val managedColTypes: Array<out DataType<*>>
        get() = _managedColTypes ?: _columns.value.let { _managedColTypes!! }

    private var _columnsByName: Map<String, StoredNamedLens<SCH, *, *>>? = null

    @Deprecated("names are now `CharSequence`s with undefined hashCode()/equals()")
    val columnsByName: Map<String, StoredNamedLens<SCH, *, *>>
        get() = _columnsByName
                ?: columns.let { cols ->
                    cols.associateByTo(newMap(cols.size), { it.name(schema).toString() })
                }.also { _columnsByName = it }


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
            (columnIndices as Map<StoredLens<SCH, *, *>, Int>)[lens]?.let { columns[it] as StoredNamedLens<SCH, T, *> }

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
@Deprecated("normal Table is Simple enough", ReplaceWith("Table"))
typealias SimpleTable<SCH, ID> = Table<SCH, ID>

@Suppress("NOTHING_TO_INLINE") // pass-through
inline fun <SCH : Schema<SCH>, ID : IdBound> tableOf(schema: SCH, name: String, idColName: String, idColType: DataType.Simple<ID>): SimpleTable<SCH, ID> =
        Table(schema, name, idColName, idColType)

@Suppress("NOTHING_TO_INLINE") // pass-through
inline fun <SCH : Schema<SCH>, ID : IdBound> tableOf(schema: SCH, name: String, idCol: ImmutableField<SCH, ID, out DataType.Simple<ID>>): SimpleTable<SCH, ID> =
        Table(schema, name, idCol)

// just extend SimpleTable, but infer type arguments instead of forcing client to specify them explicitly in object expression:

inline fun <SCH : Schema<SCH>, ID : IdBound> tableOf(
        schema: SCH, name: String, idColName: String, idColType: DataType.Simple<ID>,
        crossinline relations: SCH.() -> Array<out Relation<SCH, ID, *>>
): Table<SCH, ID> =
        object : Table<SCH, ID>(schema, name, idColName, idColType) {
            override fun relations(): Array<out Relation<SCH, ID, *>> = relations.invoke(schema)
        }

inline fun <SCH : Schema<SCH>, ID : IdBound> tableOf(
        schema: SCH, name: String, idCol: ImmutableField<SCH, ID, out DataType.Simple<ID>>,
        crossinline relations: () -> Array<out Relation<SCH, ID, *>>
): Table<SCH, ID> =
        object : Table<SCH, ID>(schema, name, idCol) {
            override fun relations(): Array<out Relation<SCH, ID, *>> = relations.invoke()
        }

@Suppress("NOTHING_TO_INLINE") // just to be consistent with other functions
inline fun <SCH : Schema<SCH>> projection(schema: SCH): Table<SCH, Nothing> =
        tableOf(schema, "<anonymous>", "<none>", nothing)

inline fun <SCH : Schema<SCH>> projection(schema: SCH, crossinline relations: SCH.() -> Array<out Relation<SCH, Nothing, *>>): Table<SCH, Nothing> =
        tableOf(schema, "<anonymous>", "<none>", nothing, relations)
