package net.aquadc.properties.internal

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater


internal fun Array<*>.with(lastElement: Any?): Array<Any?> {
    val size = size
    val new = arrayOfNulls<Any>(size + 1)
    System.arraycopy(this, 0, new, 0, size)
    new[size] = lastElement
    return new
}

@JvmName("copyOfWithoutNn")
internal fun Array<*>.copyOfWithout(idx: Int, canonicalEmptyArray: Array<Any?>): Array<Any?> =
        copyOfWithout(idx, canonicalEmptyArray as Array<Any?>?) as Array<Any?>

internal fun Array<*>.copyOfWithout(idx: Int, canonicalEmptyArray: Array<Any?>?): Array<Any?>? {
    val oldSize = size
    if (idx == 0 && oldSize == 1) return canonicalEmptyArray

    val new = arrayOfNulls<Any>(oldSize-1)
    System.arraycopy(this, 0, new, 0, idx)
    System.arraycopy(this, idx+1, new, idx, oldSize-idx-1)
    return new
}

@JvmName("withoutNullsNn")
internal fun <T : Any> Array<T?>.withoutNulls(canonicalEmptyArray: Array<T>): Array<T> =
        withoutNulls<T>(canonicalEmptyArray as Array<T>?) as Array<T>

@Suppress("UNCHECKED_CAST")
internal fun <T : Any> Array<out T?>.withoutNulls(canonicalEmptyArray: Array<T>?): Array<T>? {
    val nulls = count { it == null }
    if (nulls == 0) return this as Array<T> // it safe since there are no actual nulls

    val newSize = size - nulls
    if (newSize == 0) return canonicalEmptyArray

    val newArray = arrayOfNulls<Any>(size - nulls)
    var destPos = 0
    forEach { if (it != null) newArray[destPos++] = it }

    return newArray as Array<T> // not actually T[], but it's OK
}

/**
 * Compacts array, where null means 'empty'.
 * @return index of first empty index after compaction, or -1, if none
 * @implNote copied from [java.util.ArrayList]
 */
internal fun <T> Array<T?>.compact(): Int {
    var head = 0
    var w = 0
    while (head < size) {
        if (this[head] != null)
            this[w++] = this[head]
        head++
    }
    for (i in w until size) {
        this[i] = null
    }
    return if (w == size) -1 else w
}

// why 'arrays'? because 'updaters' if for inline functions.
internal fun <T, V> AtomicReferenceFieldUpdater<T, V>.eagerOrLazySet(thisRef: T, confinedTo: Thread?, value: V) {
    if (confinedTo == null) set(thisRef, value)
    else lazySet(thisRef, value)
}
