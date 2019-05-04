@file:Suppress("UNCHECKED_CAST") // this file is for unchecked casts :)
package net.aquadc.properties.sql

import net.aquadc.persistence.New
import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.Lens
import net.aquadc.persistence.struct.PartialStruct
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.serialized


internal typealias UpdatesMap = MutableMap<
        Pair<Table<*, *, *>, @ParameterName("colName") String>,
        MutableMap<IdBound, Any?>
        >

@Suppress("NOTHING_TO_INLINE")
internal inline fun UpdatesMap() = New.map<
        Pair<Table<*, *, *>, @ParameterName("colName") String>,
        MutableMap<IdBound, Any?>
        >()

internal fun <SCH : Schema<SCH>, ID : IdBound> UpdatesMap.getFor(table: Table<SCH, ID, *>, colName: String): MutableMap<ID, Any?>? =
        get(table to colName) as MutableMap<ID, Any?>?

internal fun <SCH : Schema<SCH>, T, ID : IdBound> UpdatesMap.put(table: Table<SCH, ID, *>, colName: String, value: T, id: ID) {
    (this as MutableMap<Pair<Table<SCH, *, *>, String>, MutableMap<ID, Any?>>).getOrPut(table to colName, New::map)[id] = value
}

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

internal inline fun <T, U> forEachOfBoth(left: List<T>, right: List<U>, block: (Int, T, U) -> Unit) {
    val size = left.size
    check(right.size == size)
    for (i in 0 until size) {
        block(i, left[i], right[i])
    }
}
