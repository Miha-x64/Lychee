@file:Suppress("UNCHECKED_CAST") // this file is for unchecked casts :)
package net.aquadc.properties.sql

import net.aquadc.persistence.New
import net.aquadc.persistence.struct.Lens
import net.aquadc.persistence.struct.NamedLens
import net.aquadc.persistence.struct.PartialStruct
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.serialized


internal typealias UpdatesMap = MutableMap<
        Table<*, *, *>,
        MutableMap<
                IdBound,
                @ParameterName("valuesByOrdinal") Array<Any?>
                >
        >

@Suppress("NOTHING_TO_INLINE")
internal inline fun UpdatesMap() = New.map<
        Table<*, *, *>,
        MutableMap<
                IdBound,
                Array<Any?>
                >
        >()

@Suppress("UPPER_BOUND_VIOLATED")
internal inline val Table<*, *, *>.erased
    get() = this as Table<Any, IdBound, Record<Any, IdBound>>

internal inline val DataType<*>.erased
    get() = this as DataType<Any?>

@Suppress("UPPER_BOUND_VIOLATED")
internal inline val <SCH : Schema<SCH>, STR : PartialStruct<SCH>, T> Lens<SCH, STR, T>.erased
    get() = this as Lens<Schema<*>, PartialStruct<*>?, Any?>

internal inline fun <T, R> DataType<T>.flattened(func: (isNullable: Boolean, simple: DataType.Simple<T>) -> R): R =
        when (this) {
            is DataType.Nullable<*> -> {
                when (val actualType = actualType as DataType<T>) {
                    is DataType.Nullable<*> -> throw AssertionError()
                    is DataType.Simple -> func(true, actualType)
                    is DataType.Collect<*, *>,
                    is DataType.Partial<*, *> -> func(true, serialized(actualType))
                }
            }
            is DataType.Simple -> func(false, this)
            is DataType.Collect<*, *>,
            is DataType.Partial<*, *> -> func(false, serialized(this))
        }

internal inline fun <SCH : Schema<SCH>> bindQueryParams(
        condition: WhereCondition<SCH>, table: Table<SCH, *, *>, bind: (DataType<Any?>, idx: Int, value: Any?) -> Unit
) {
    val size = condition.size
    if (size > 0) {
        val argCols = arrayOfNulls<Lens<SCH, *, *>>(size)
        val argValues = arrayOfNulls<Any>(size)
        condition.setValuesTo(0, argCols, argValues)
        val cols = table.columns
        val indices = table.columnIndices
        for (i in 0 until size) {
            val colIndex = indices[argCols[i]]!!
            val column = cols[colIndex]
            check(argCols[i]!!.type == column.type)
            // I hope `argCols[i].type` is the same as `column.type`, but let's overcare :)

            bind(column.type as DataType<Any?>, i, argValues[i])
            // erase its type and assume that caller is clever enough
        }
    }
}

internal inline fun <SCH : Schema<SCH>> bindInsertionParams(table: Table<SCH, *, *>, data: Struct<SCH>, bind: (DataType<Any?>, idx: Int, value: Any?) -> Unit) {
    val offset = if (table.pkField === null) 1 else 0
    val cols = table.columns
    for (i in 0 until cols.size - offset) {
        val col = cols[i + offset].erased
        bind(col.type, i, col(data))
    }
}

internal inline fun <SCH : Schema<SCH>, ID : IdBound> bindUpdateParams(
        table: Table<SCH, ID, *>, id: ID, columns: Any, values: Any?,
        bind: (DataType<Any?>, idx: Int, value: Any?) -> Unit
) {
    val colCount = if (columns is Array<*>) {
        columns as Array<NamedLens<SCH, Struct<SCH>, *>>
        values as Array<*>?
        columns.forEachIndexed { i, col ->
            bind(col.type.erased, i, values?.get(i))
        }
        columns.size
    } else {
        bind((columns as NamedLens<SCH, Struct<SCH>, *>).type.erased, 0, values)
        1
    }
    bind(table.idColType as DataType<Any?>, colCount, id)
}
