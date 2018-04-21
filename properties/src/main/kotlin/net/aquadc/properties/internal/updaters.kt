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
