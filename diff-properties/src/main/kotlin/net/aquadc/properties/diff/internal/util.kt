package net.aquadc.properties.diff.internal

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater

// main module contains the same thing, but it's internal, too
internal inline fun <T, V> AtomicReferenceFieldUpdater<T, V>.update(zis: T, update: (V) -> V) {
    var prev: V
    var next: V
    do {
        prev = get(zis)
        next = update(prev)
    } while (!compareAndSet(zis, prev, next))
}
