@file:Suppress("UNCHECKED_CAST") // this file is for unchecked casts :)
package net.aquadc.properties.sql

import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.serialized


internal typealias UpdatesHashMap = HashMap<
        Pair<Table<*, *, *>, FieldDef.Mutable<*, *>>,
        HashMap<IdBound, Any?>
        >

internal fun <SCH : Schema<SCH>, T, ID : IdBound> UpdatesHashMap.getFor(table: Table<SCH, ID, *>, col: FieldDef.Mutable<SCH, T>): HashMap<ID, T>? =
        get(table to col) as HashMap<ID, T>?

internal fun <SCH : Schema<SCH>, T, ID : IdBound> UpdatesHashMap.put(table: Table<SCH, ID, *>, field: FieldDef<SCH, T>, value: T, id: ID) {
    (this as HashMap<Pair<Table<SCH, *, *>, FieldDef<SCH, T>>, HashMap<ID, Any?>>).getOrPut(table to field, ::HashMap)[id] = value
}

@Suppress("UPPER_BOUND_VIOLATED")
internal inline val Table<*, *, *>.erased
    get() = this as Table<Any, IdBound, Record<Any, IdBound>>

@Suppress("UPPER_BOUND_VIOLATED")
internal inline val FieldDef.Mutable<*, *>.erased
    get() = this as FieldDef.Mutable<Any, Any?>

internal inline val DataType<*>.erased
    get() = this as DataType<Any?>

internal inline fun <T, R> DataType<T>.flattened(func: (isNullable: Boolean, simple: DataType.Simple<T>) -> R): R =
        when (this) {
            is DataType.Nullable<*> -> {
                val actualType = actualType as DataType<T>
                when (actualType) {
                    is DataType.Nullable<*> -> throw AssertionError()
                    is DataType.Simple -> func(true, actualType)
                    is DataType.Collect<*, *> -> func(true, serialized(actualType))
                }
            }
            is DataType.Simple -> func(false, this)
            is DataType.Collect<*, *> -> func(false, serialized(this))
        }
