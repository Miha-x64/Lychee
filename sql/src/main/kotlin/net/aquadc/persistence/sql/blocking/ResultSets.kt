@file:JvmName("ResultSets")
package net.aquadc.persistence.sql.blocking

import net.aquadc.persistence.CloseableIterator
import net.aquadc.persistence.NullSchema
import net.aquadc.persistence.castNull
import net.aquadc.persistence.fatMapTo
import net.aquadc.persistence.newMap
import net.aquadc.persistence.sql.BindBy
import net.aquadc.persistence.sql.Embedded
import net.aquadc.persistence.sql.JdbcType
import net.aquadc.persistence.sql.Simple
import net.aquadc.persistence.sql.Table
import net.aquadc.persistence.sql.compute
import net.aquadc.persistence.sql.dialect.foldArrayType
import net.aquadc.persistence.sql.forceIndexOfManaged
import net.aquadc.persistence.sql.inflate
import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.struct.StructSnapshot
import net.aquadc.persistence.type.AnyCollection
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.Ilk
import net.aquadc.persistence.type.serialized
import java.sql.ResultSet


// cell

/**
 * Fetch a single cell as [type] at the given column [index]₁ out of [this] [ResultSet],
 * assuming that it is standing on a row.
 */
@JvmOverloads fun <T> ResultSet.cell(type: Ilk<out T, *>, index: Int = 1): T =
    cell(type, index, false)

@Suppress("unused", "UnusedReceiverParameter")
@Deprecated("single cell can't hold a Collection unless nativeType: Ilk is used", level = DeprecationLevel.ERROR)
@JvmOverloads fun ResultSet.cell(type: DataType.NotNull.Collect<*, *, *>, index: Int = 1): Nothing = throw AssertionError()

@Suppress("unused", "UnusedReceiverParameter")
@Deprecated("single cell can't hold a Partial/Struct unless nativeType: Ilk is used", level = DeprecationLevel.ERROR)
@JvmOverloads fun ResultSet.cell(type: DataType.NotNull.Partial<*, *>, index: Int = 1): Nothing = throw AssertionError()

@Suppress("unused", "UnusedReceiverParameter")
@Deprecated("single cell can't hold a Collection unless nativeType: Ilk is used", level = DeprecationLevel.ERROR)
@JvmOverloads fun ResultSet.cell(type: DataType.Nullable<*, DataType.NotNull.Collect<*, *, *>>, index: Int = 1): Nothing = throw AssertionError()

@JvmName("nsCell")
@Suppress("unused", "UnusedReceiverParameter")
@Deprecated("single cell can't hold a Partial/Struct unless nativeType: Ilk is used", level = DeprecationLevel.ERROR)
@JvmOverloads fun ResultSet.cell(type: DataType.Nullable<*, DataType.NotNull.Partial<*, *>>, index: Int = 1): Nothing = throw AssertionError()

@PublishedApi internal fun <T> ResultSet.cell(
    type: Ilk<out T, *>,
    index: Int,
    hasArraySupport: Boolean,
): T {
    (type.platformType as? JdbcType<T, *>)?.let {
        return it.load(this, index)
    }

    val unwrapped = type.type as DataType<T>
    val nullable: Boolean
    val type =
        if (unwrapped is DataType.Nullable<*, *>) { nullable = true; unwrapped.actualType as DataType.NotNull<T> }
        else { nullable = false; unwrapped as DataType.NotNull<T> }
    return when (type) {
        is DataType.NotNull.Simple ->
            simpleCell(type, nullable, index)
        is DataType.NotNull.Collect<T, *, *> ->
            arrayCell(type, index, nullable, hasArraySupport) // Internal feature used by JdbcSession
        is DataType.NotNull.Partial<T, *> ->
            throw AssertionError()
    }
}

private fun <T> ResultSet.simpleCell(
    type: DataType.NotNull.Simple<T>,
    nullable: Boolean,
    index: Int,
): T {
    val value = when (type.kind) {
        DataType.NotNull.Simple.Kind.Bool -> getBoolean(index)
        DataType.NotNull.Simple.Kind.I32 -> getInt(index)
        DataType.NotNull.Simple.Kind.I64 -> getLong(index)
        DataType.NotNull.Simple.Kind.F32 -> getFloat(index)
        DataType.NotNull.Simple.Kind.F64 -> getDouble(index)
        DataType.NotNull.Simple.Kind.Str -> getString(index)
        DataType.NotNull.Simple.Kind.Blob -> getBytes(index)
    }
    // must check, will get 0, 0L, 0f, 0d otherwise
    return if (wasNull()) castNull(nullable) { errorLocation(type, index) } else type.load(value!!)
}

private fun <T> ResultSet.arrayCell(
    type: DataType.NotNull.Collect<T, *, *>,
    index: Int,
    nullable: Boolean,
    hasArraySupport: Boolean
): T = foldArrayType(
    hasArraySupport, type.elementType,
    { nullable, elT ->
        val arr = getArray(index)
        if (wasNull()) castNull(nullable) { errorLocation(type, index) }
        else fromArray(type, index, arr.array, nullable, elT)
    },
    {
        val obj = getObject(index)
        if (wasNull()) castNull(nullable) { errorLocation(type, index) }
        else serialized(type).load(obj)
    }
)

private fun <T> fromArray(
    type: DataType.NotNull.Collect<T, *, *>,
    index: Int,
    value: AnyCollection,
    nullable: Boolean,
    elT: DataType.NotNull.Simple<*>,
): T =
    // PGarray is always Object[], unboxing optimization is useless
    // but pass-through one still can be implemented
    type.load(value.fatMapTo(::ArrayList) { it: Any? ->
        if (it == null) castNull(nullable) { errorLocation(elT, index) } else elT.load(it)
    })

private fun errorLocation(type: DataType<*>, index: Int): String = "$type at $index₁"


// column

/**
 * Fetch all cells of [type] at a given column [index]₁ out of [this] [ResultSet].
 */
@JvmOverloads fun <T> ResultSet.asColumnIterator(type: Ilk<out T, *>, index: Int = 1): CloseableIterator<T> =
    object : ResultSetIterator<NullSchema, T>(NullSchema, this) {
        override fun row(cur: ResultSet): T =
            cur.cell(type, index)
    }

@Suppress("unused", "UnusedReceiverParameter")
@Deprecated("single column can't hold a Collection unless nativeType: Ilk is used", level = DeprecationLevel.ERROR)
@JvmOverloads fun ResultSet.asColumnIterator(type: DataType.NotNull.Collect<*, *, *>, index: Int = 1): Nothing = throw AssertionError()

@Suppress("unused", "UnusedReceiverParameter")
@Deprecated("single column can't hold a Partial/Struct unless nativeType: Ilk is used", level = DeprecationLevel.ERROR)
@JvmOverloads fun ResultSet.asColumnIterator(type: DataType.NotNull.Partial<*, *>, index: Int = 1): Nothing = throw AssertionError()

@Suppress("unused", "UnusedReceiverParameter")
@Deprecated("single column can't hold a Collection unless nativeType: Ilk is used", level = DeprecationLevel.ERROR)
@JvmOverloads fun ResultSet.asColumnIterator(type: DataType.Nullable<*, DataType.NotNull.Collect<*, *, *>>, index: Int = 1): Nothing = throw AssertionError()

@JvmName("nsColumn")
@Suppress("unused", "UnusedReceiverParameter")
@Deprecated("single column can't hold a Partial/Struct unless nativeType: Ilk is used", level = DeprecationLevel.ERROR)
@JvmOverloads fun ResultSet.asColumnIterator(type: DataType.Nullable<*, DataType.NotNull.Partial<*, *>>, index: Int = 1): Nothing = throw AssertionError()


// structs

fun <SCH : Schema<SCH>> ResultSet.rowAsStruct(
    type: Table<SCH, *>,
    bindBy: BindBy = BindBy.Name,
): StructSnapshot<SCH> {
    val values = arrayOfNulls<Any>(type.managedColNames.size)
    unembed(values, this, null, 0, type.managedColNames, type._managedColTypes!!, bindBy)
    inflate(type._recipe!!, values, 0, 0, 0)
    @Suppress("UNCHECKED_CAST") // we know that Table<SCH>.recipe will give us StructSnapshot<SCH>
    return values[0] as StructSnapshot<SCH>
}

@JvmOverloads
fun <SCH : Schema<SCH>> ResultSet.asStructIterator(
    type: Table<SCH, *>,
    bindBy: BindBy = BindBy.Name,
    transient: Boolean = false,
): CloseableIterator<Struct<SCH>> =
    ResultSetStructIterator(
        this, type, bindBy,
        hasArraySupport = true, // hasArraySupport=false is a questionable option using serialized()
        transient = transient,
    )


// iter impl

internal open class ResultSetStructIterator<SCH : Schema<SCH>>(
    initial: ResultSet?,
    private val table: Table<SCH, *>,
    private val bindBy: BindBy,
    private val hasArraySupport: Boolean,
    private val transient: Boolean,
) : ResultSetIterator<SCH, Struct<SCH>>(table.schema, initial) {
    private var arr: Array<Any?>? = null
    private fun arr(size: Int) = arr?.takeIf { it.size >= size } ?: arrayOfNulls<Any>(size).also { arr = it }
    private var colIndices = if (bindBy == BindBy.Name) newMap<String, Int>(table.managedColNames.size) else null
    final override fun <T> cell(field: FieldDef<SCH, T, *>): T =
        when (val delegate = table.delegateFor(field)) {
            is Simple<SCH, *> -> {
                val type = table.typeOf(field)
                cur.cell(
                    type,
                    when (bindBy) {
                        BindBy.Name ->
                            field.name(table.schema).toString().let { key ->
                                colIndices!!.getOrPut(key) { cur.findColumn(key) }
                            }
                        BindBy.Position ->
                            forceIndexOfManaged(table, field) + 1
                    },
                    hasArraySupport,
                )
            }
            is Embedded<SCH, *> -> {
                val values = arr(delegate.columnNames.size)
                unembed(values, cur, colIndices, delegate.myOffset, delegate.columnNames, delegate.columnTypes, bindBy)
                inflate(delegate.recipe, values, 0, 0, 0)
                values[0] as T
            }
        }
    final override fun row(cur: ResultSet): Struct<SCH> =
        if (transient) this else StructSnapshot(this)
    final override fun onClose() {
        super.onClose()
        arr = null
        colIndices = null
    }
}

private fun unembed(
    into: Array<Any?>, cur: ResultSet, colIndices: MutableMap<String, Int>?,
    offset: Int, columnNames: Array<out CharSequence>, columnTypes: Array<out Ilk<*, *>>, bindBy: BindBy
) {
    when (bindBy) {
        BindBy.Name ->
            repeat(columnTypes.size) { idx ->
                into[idx] = cur.cell(
                    columnTypes[idx],
                    columnNames[idx].toString().let { key ->
                        colIndices.compute(key, cur::findColumn)
                    },
                )
            }

        BindBy.Position ->
            repeat(columnTypes.size) { idx ->
                into[idx] = cur.cell(columnTypes[idx], offset + idx + 1)
            }
    }
}

internal abstract class ResultSetIterator<SCH : Schema<SCH>, R>(
    schema: SCH, initial: ResultSet?,
) : DbIter<ResultSet, SCH, R>(schema, initial) {
    final override fun moveToNext(): Boolean = cur.next()
    override fun onClose() = cur.close()
}
