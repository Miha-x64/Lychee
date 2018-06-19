package net.aquadc.properties.internal

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater

// UpdatersKt class wont't ever be loaded because it fully consists of inline functions

internal inline fun <T, V> AtomicReferenceFieldUpdater<T, V>.update(zis: T, update: (V) -> V) {
    var prev: V
    var next: V
    do {
        prev = get(zis)
        next = update(prev)
    } while (!compareAndSet(zis, prev, next))
}

internal inline fun <T, V> AtomicReferenceFieldUpdater<T, V>.updateUndGet(zis: T, update: (V) -> V): V {
    var prev: V
    var next: V
    do {
        prev = get(zis)
        next = update(prev)
    } while (!compareAndSet(zis, prev, next))
    return next
}

internal inline fun <T, V> AtomicReferenceFieldUpdater<T, V>.getUndUpdate(obj: T, update: (V) -> V): V {
    var prev: V
    var next: V
    do {
        prev = get(obj)
        next = update(prev)
    } while (!compareAndSet(obj, prev, next))
    return prev
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun Array<*>.copyOfWithout(idx: Int, canonicalEmptyArray: Array<Any?>): Array<Any?> =
        copyOfWithout(idx, canonicalEmptyArray as Array<Any?>?) as Array<Any?>

@Suppress("NOTHING_TO_INLINE")
internal inline fun <T : Any> Array<T?>.withoutNulls(canonicalEmptyArray: Array<T>): Array<T> =
        withoutNulls<T>(canonicalEmptyArray as Array<T>?) as Array<T>

internal inline fun <T> AList(size: Int, init: (Int) -> T): List<T> {
    val a = arrayOfNulls<Any>(size) // Array(size, init) calls arraylength on itself, this fun doesn't
    repeat(size) { a[it] = init(it) }
    @Suppress("UNCHECKED_CAST")
    return (a as Array<T>).asList()
}
