@file:JvmName("Cursors")
package net.aquadc.persistence.sql.blocking

import android.database.Cursor
import net.aquadc.persistence.castNull
import net.aquadc.persistence.sql.flattened
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
