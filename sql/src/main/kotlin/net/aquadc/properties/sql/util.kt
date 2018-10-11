@file:Suppress("UNCHECKED_CAST") // this file is for unchecked casts :)
package net.aquadc.properties.sql

import net.aquadc.persistence.type.DataType


internal typealias UpdatesHashMap = HashMap<MutableCol<*, *>, HashMap<Long, Any?>>

internal fun <TBL : Table<TBL, *, *>, T> UpdatesHashMap.getFor(col: MutableCol<TBL, T>): HashMap<Long, T>? =
        get(col) as HashMap<Long, T>?

internal fun <TBL : Table<TBL, *, *>, T> UpdatesHashMap.put(cv: ColValue<TBL, T>, localId: Long) {
    (this as HashMap<Col<TBL, T>, HashMap<Long, Any?>>).getOrPut(cv.col, ::HashMap)[localId] = cv.value
}

@Suppress("UPPER_BOUND_VIOLATED")
internal inline val Table<*, *, *>.erased
    get() = this as Table<Any, IdBound, Record<Any, IdBound>>

@Suppress("UPPER_BOUND_VIOLATED")
internal inline val MutableCol<*, *>.erased
    get() = this as MutableCol<Any, Any?>

internal inline val DataType<*>.erased
    get() = this as DataType<Any?>
