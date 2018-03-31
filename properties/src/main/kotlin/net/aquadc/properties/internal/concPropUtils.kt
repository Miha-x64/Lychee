package net.aquadc.properties.internal

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater


internal inline fun <T, V> AtomicReferenceFieldUpdater<T, V>.update(zis: T, update: (V) -> V) {
    var prev: V
    var next: V
    do {
        prev = get(zis)
        next = update(prev)
    } while (!compareAndSet(zis, prev, next))
}

// 'i' is for 'inline', don't shadow 'getAndUpdate'
internal inline fun <T, V> AtomicReferenceFieldUpdater<T, V>.iGetAndUpdate(zis: T, update: (V) -> V): V {
    var prev: V
    var next: V
    do {
        prev = get(zis)
        next = update(prev)
    } while (!compareAndSet(zis, prev, next))
    return prev
}

internal fun Array<*>.with(lastElement: Any?): Array<Any?> {
    val size = size
    val new = arrayOfNulls<Any>(size + 1)
    System.arraycopy(this, 0, new, 0, size)
    new[size] = lastElement
    return new
}

internal fun Array<*>.copyOfWithout(idx: Int, canonicalEmptyArray: Array<Any?>): Array<Any?> {
    val oldSize = size
    if (idx == 0 && oldSize == 1) return canonicalEmptyArray

    val new = arrayOfNulls<Any>(oldSize-1)
    System.arraycopy(this, 0, new, 0, idx)
    System.arraycopy(this, idx+1, new, idx, oldSize-idx-1)
    return new
}

internal fun Array<Any?>.withoutNulls(canonicalEmptyArray: Array<Any?>): Array<Any?> {
    val nulls = count { it == null }
    if (nulls == 0) return this

    val newSize = size - nulls
    if (newSize == 0) return canonicalEmptyArray

    val newArray = arrayOfNulls<Any>(size - nulls)
    var destPos = 0
    forEach { if (it != null) newArray[destPos++] = it }

    return newArray
}
