@file:JvmName("Cursors")
package net.aquadc.persistence.sql.blocking

import android.database.Cursor
import net.aquadc.persistence.CloseableIterator
import net.aquadc.persistence.CloseableSizedIterator
import net.aquadc.persistence.NullSchema
import net.aquadc.persistence.castNull
import net.aquadc.persistence.sql.flattened
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.type.DataType


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


// iter impl

internal abstract class CursorIterator<SCH : Schema<SCH>, R>(
    schema: SCH, initial: Cursor?,
) : DbIter<Cursor, SCH, R>(schema, initial), CloseableSizedIterator<R> {
    override val size: Int get() = cur.count
    override fun moveToNext(): Boolean = cur.moveToNext()
    override fun onClose() = cur.close()
}
