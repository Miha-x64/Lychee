@file:Suppress("UNCHECKED_CAST") // this file is for unchecked casts :)
package net.aquadc.properties.sql

import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.type.DataType


internal typealias UpdatesHashMap = HashMap<
        Pair<Table<*, *, *>, FieldDef.Mutable<*, *>>,
        HashMap<Long, Any?>
        >

internal fun <SCH : Schema<SCH>, T> UpdatesHashMap.getFor(table: Table<SCH, *, *>, col: FieldDef.Mutable<SCH, T>): HashMap<Long, T>? =
        get(table to col) as HashMap<Long, T>?

internal fun <SCH : Schema<SCH>, T> UpdatesHashMap.put(table: Table<SCH, *, *>, cv: ColValue<SCH, T>, localId: Long) {
    (this as HashMap<Pair<Table<SCH, *, *>, FieldDef<SCH, T>>, HashMap<Long, Any?>>).getOrPut(table to cv.col, ::HashMap)[localId] = cv.value
}

@Suppress("UPPER_BOUND_VIOLATED")
internal inline val Table<*, *, *>.erased
    get() = this as Table<Any, IdBound, Record<Any, IdBound>>

@Suppress("UPPER_BOUND_VIOLATED")
internal inline val FieldDef.Mutable<*, *>.erased
    get() = this as FieldDef.Mutable<Any, Any?>

internal inline val DataType<*>.erased
    get() = this as DataType<Any?>
