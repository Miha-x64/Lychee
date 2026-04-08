@file:JvmName("Cursors")
package net.aquadc.persistence.sql.blocking

import android.database.Cursor
import net.aquadc.persistence.CloseableIterator
import net.aquadc.persistence.CloseableSizedIterator
import net.aquadc.persistence.NullSchema
import net.aquadc.persistence.castNull
import net.aquadc.persistence.eq
import net.aquadc.persistence.newMap
import net.aquadc.persistence.sql.BindBy
import net.aquadc.persistence.sql.Embedded
import net.aquadc.persistence.sql.Simple
import net.aquadc.persistence.sql.Table
import net.aquadc.persistence.sql.compute
import net.aquadc.persistence.sql.flattened
import net.aquadc.persistence.sql.forceIndexOfManaged
import net.aquadc.persistence.sql.inflate
import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.struct.StructSnapshot
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.Ilk


// cell

/**
 * Fetch a single cell as [type] at the given column [index]₀ out of [this] [Cursor],
 * assuming that it is standing on a row.
 */
@JvmOverloads fun <T> Cursor.cell(type: DataType<out T>, index: Int = 0): T =
    type.flattened { isNullable, simple ->
        if (isNull(index))
            castNull(isNullable) { "$type at $index" }
        else simple.load(
            when (simple.kind) {
                DataType.NotNull.Simple.Kind.Bool -> getInt(index) == 1
                DataType.NotNull.Simple.Kind.I32 -> getInt(index)
                DataType.NotNull.Simple.Kind.I64 -> getLong(index)
                DataType.NotNull.Simple.Kind.F32 -> getFloat(index)
                DataType.NotNull.Simple.Kind.F64 -> getDouble(index)
                DataType.NotNull.Simple.Kind.Str -> getString(index)
                DataType.NotNull.Simple.Kind.Blob -> getBlob(index)
            }
        )
    }

@Suppress("unused", "UnusedReceiverParameter")
@Deprecated("single cell can't hold a Collection", level = DeprecationLevel.ERROR)
@JvmOverloads fun Cursor.cell(type: DataType.NotNull.Collect<*, *, *>, index: Int = 0): Nothing = throw AssertionError()

@Suppress("unused", "UnusedReceiverParameter")
@Deprecated("single cell can't hold a Partial/Struct", level = DeprecationLevel.ERROR)
@JvmOverloads fun Cursor.cell(type: DataType.NotNull.Partial<*, *>, index: Int = 0): Nothing = throw AssertionError()

@Suppress("unused", "UnusedReceiverParameter")
@Deprecated("single cell can't hold a Collection", level = DeprecationLevel.ERROR)
@JvmOverloads fun Cursor.cell(type: DataType.Nullable<*, DataType.NotNull.Collect<*, *, *>>, index: Int = 0): Nothing = throw AssertionError()

@JvmName("nsCell")
@Suppress("unused", "UnusedReceiverParameter")
@Deprecated("single cell can't hold a Partial/Struct", level = DeprecationLevel.ERROR)
@JvmOverloads fun Cursor.cell(type: DataType.Nullable<*, DataType.NotNull.Partial<*, *>>, index: Int = 0): Nothing = throw AssertionError()


// column

/**
 * Fetch all cells of [type] at a given column [index]₀ out of [this] [Cursor].
 */
@JvmOverloads fun <T> Cursor.asColumnIterator(type: DataType<out T>, index: Int = 0): CloseableIterator<T> =
    object : CursorIterator<NullSchema, T>(NullSchema, this) {
        override fun row(cur: Cursor): T =
            cur.cell(type, index)
    }

@Suppress("unused", "UnusedReceiverParameter")
@Deprecated("single column can't hold a Collection", level = DeprecationLevel.ERROR)
@JvmOverloads fun Cursor.asColumnIterator(type: DataType.NotNull.Collect<*, *, *>, index: Int = 1): Nothing = throw AssertionError()

@Suppress("unused", "UnusedReceiverParameter")
@Deprecated("single column can't hold a Partial/Struct", level = DeprecationLevel.ERROR)
@JvmOverloads fun Cursor.asColumnIterator(type: DataType.NotNull.Partial<*, *>, index: Int = 1): Nothing = throw AssertionError()

@Suppress("unused", "UnusedReceiverParameter")
@Deprecated("single column can't hold a Collection", level = DeprecationLevel.ERROR)
@JvmOverloads fun Cursor.asColumnIterator(type: DataType.Nullable<*, DataType.NotNull.Collect<*, *, *>>, index: Int = 1): Nothing = throw AssertionError()

@JvmName("nsColumn")
@Suppress("unused", "UnusedReceiverParameter")
@Deprecated("single column can't hold a Partial/Struct", level = DeprecationLevel.ERROR)
@JvmOverloads fun Cursor.asColumnIterator(type: DataType.Nullable<*, DataType.NotNull.Partial<*, *>>, index: Int = 1): Nothing = throw AssertionError()


// structs

fun <SCH : Schema<SCH>> Cursor.rowAsStruct(
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
fun <SCH : Schema<SCH>> Cursor.asStructIterator(
    type: Table<SCH, *>,
    bindBy: BindBy = BindBy.Name,
    transient: Boolean = false,
): CloseableSizedIterator<Struct<SCH>> =
    CursorStructIterator<SCH>(this, type, bindBy, transient)


// iter impl

internal open class CursorStructIterator<SCH : Schema<SCH>>(
    initial: Cursor?,
    private val table: Table<SCH, *>,
    private val bindBy: BindBy,
    private val transient: Boolean,
) : CursorIterator<SCH, Struct<SCH>>(table.schema, initial) {
    private var arr: Array<Any?>? = null
    private fun arr(size: Int) = arr?.takeIf { it.size >= size } ?: arrayOfNulls<Any>(size).also { arr = it }
    private var colIndices = if (bindBy == BindBy.Name) newMap<String, Int>(table.managedColNames.size) else null
    final override fun <T> cell(field: FieldDef<SCH, T, *>): T =
        when (val delegate = table.delegateFor(field)) {
            is Simple<SCH, *> -> {
                val type = table.typeOf(field).type
                cur.cell(
                    type,
                    when (bindBy) {
                        BindBy.Name ->
                            field.name(table.schema).toString().let { key ->
                                colIndices!!.getOrPut(key) { cur.getColIdx(Int.MAX_VALUE, key) }
                            }
                        BindBy.Position ->
                            forceIndexOfManaged(table, field)
                    },
                )
            }
            is Embedded<SCH, *> -> {
                val values = arr(delegate.columnNames.size)
                unembed(values, cur, colIndices, delegate.myOffset, delegate.columnNames, delegate.columnTypes, bindBy)
                inflate(delegate.recipe, values, 0, 0, 0)
                values[0] as T
            }
        }
    final override fun row(cur: Cursor): Struct<SCH> =
        if (transient) this else StructSnapshot(this)
    final override fun onClose() {
        super.onClose()
        arr = null
        colIndices = null
    }
}

private fun unembed(
    into: Array<Any?>, cur: Cursor, colIndices: MutableMap<String, Int>?,
    offset: Int, columnNames: Array<out CharSequence>, columnTypes: Array<out Ilk<*, *>>, bindBy: BindBy
) {
    when (bindBy) {
        BindBy.Name ->
            repeat(columnTypes.size) { idx ->
                into[idx] = cur.cell(
                    columnTypes[idx] as DataType<*>,
                    columnNames[idx].toString().let { key ->
                        colIndices.compute(key) { cur.getColIdx(idx, it) }
                    },
                )
            }
        BindBy.Position ->
            repeat(columnTypes.size) { idx ->
                into[idx] = cur.cell(columnTypes[idx] as DataType<*>, offset + idx)
            }
    }
}

internal fun Cursor.getColIdx(guess: Int, name: CharSequence): Int { // native `getColumnIndex` wrecks labels with '.'!
    val columnNames = columnNames!! // FIXME faster
    if (columnNames.size > guess && name.eq(columnNames[guess], false)) return guess
    val idx = columnNames.indexOfFirst { name.eq(it, false) }
    if (idx < 0) error { "$name !in ${columnNames.contentToString()}" }
    return idx
}

internal abstract class CursorIterator<SCH : Schema<SCH>, R>(
    schema: SCH, initial: Cursor?,
) : DbIter<Cursor, SCH, R>(schema, initial), CloseableSizedIterator<R> {
    final override val size: Int get() = cur.count
    final override fun moveToNext(): Boolean = cur.moveToNext()
    override fun onClose() = cur.close()
}
