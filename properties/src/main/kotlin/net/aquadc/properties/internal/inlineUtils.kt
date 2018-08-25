package net.aquadc.properties.internal

import java.util.concurrent.atomic.AtomicReference

// InlineUtilsKt class wont't ever be loaded because it fully consists of inline functions

internal inline fun <V> AtomicReference<V>.update(update: (V) -> V) {
    var prev: V
    var next: V
    do {
        prev = get()
        next = update(prev)
    } while (!compareAndSet(prev, next))
}

internal inline fun <V> AtomicReference<V>.updateUndGet(update: (V) -> V): V {
    var prev: V
    var next: V
    do {
        prev = get()
        next = update(prev)
    } while (!compareAndSet(prev, next))
    return next
}

internal inline fun <V> AtomicReference<V>.getUndUpdate(update: (V) -> V): V {
    var prev: V
    var next: V
    do {
        prev = get()
        next = update(prev)
    } while (!compareAndSet(prev, next))
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

@Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST") @JvmSynthetic
internal inline fun <T> unset(): T = Unset as T
