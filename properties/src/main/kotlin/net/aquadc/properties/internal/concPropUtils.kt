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

internal inline fun <reified T> Array<T>.with(lastElement: T): Array<T> {
    val size = size
    val new = arrayOfNulls<T>(size + 1)
    System.arraycopy(this, 0, new, 0, size)
    new[size] = lastElement
    return new as Array<T> // if T is not nullable, this array element type won't be nullable too
}

internal inline fun <reified T> Array<T>.copyOfWithout(idx: Int): Array<T> {
    val oldSize = size
    val new = arrayOfNulls<T>(oldSize-1)
    System.arraycopy(this, 0, new, 0, idx)
    System.arraycopy(this, idx+1, new, idx, oldSize-idx-1)
    return new as Array<T> // same here
}

internal inline fun <reified T> Array<T>.withoutNulls(): Array<T> {
    val nulls = count { it == null }
    if (nulls == 0) return this

    val newSize = size - nulls
    if (newSize == 0) return emptyArray()

    val newArray = arrayOfNulls<T>(size - nulls)
    var destPos = 0
    forEach { if (it != null) newArray[destPos++] = it }

    return newArray as Array<T>
}
