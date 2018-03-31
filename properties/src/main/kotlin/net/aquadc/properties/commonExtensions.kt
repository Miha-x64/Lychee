@file:Suppress("NOTHING_TO_INLINE")
package net.aquadc.properties

import net.aquadc.properties.internal.ConcDistinctPropertyWrapper
import net.aquadc.properties.internal.ConcDebouncedProperty
import net.aquadc.properties.internal.UnsDistinctPropertyWrapper
import net.aquadc.properties.internal.UnsDebouncedProperty
import java.util.concurrent.TimeUnit

inline val <T> Property<T>.value
    get() = this.getValue()

/*inline*/ var <T> MutableProperty<T>.value
    get() = this.getValue()
    set(new) { this.setValue(new) }

inline fun <T> Property<T>.readOnlyView() = map { it }

inline fun <T> Property<T>.distinct(noinline areEqual: (T, T) -> Boolean) = when {
    !this.mayChange -> this
    !isConcurrent -> UnsDistinctPropertyWrapper(this, areEqual)
    else -> ConcDistinctPropertyWrapper(this, areEqual)
}

object Identity : (Any?, Any?) -> Boolean {
    override fun invoke(p1: Any?, p2: Any?): Boolean = p1 === p2
}

object Equals : (Any?, Any?) -> Boolean {
    override fun invoke(p1: Any?, p2: Any?): Boolean = p1 == p2
}

/**
 * Calls [func] for each [Property.value] including initial.
 */
fun <T> Property<T>.onEach(func: (T) -> Unit) {
    addChangeListener { _, new -> func(new) }
    // *
    func(value)

    /*
    What's better — call func first or add listener first?
    a) if we call function first, in (*) place value may change and we won't see new one
    b) if we add listener first, in (*) place value may change and we'll call func after that, in wrong order
     */
}

/**
 * Returns debounced wrapper around this property.
 * Will work only on those threads which can accept tasks (currently JavaFX and Android Looper supported).
 * Single-threaded debounced wrapper will throw exception when created on wrong thread.
 * Concurrent debounced wrapper will throw exception when listener gets subscribed from wrong thread.
 */
fun <T> Property<T>.debounced(delay: Long, unit: TimeUnit) = when {
    !mayChange -> this
    isConcurrent -> ConcDebouncedProperty(this, delay, unit)
    !isConcurrent -> UnsDebouncedProperty(this, delay, unit)
    else -> throw AssertionError()
}

/**
 * Inline copy of [java.util.concurrent.atomic.AtomicReference.getAndUpdate].
 */
inline fun <T> MutableProperty<T>.getAndUpdate(updater: (old: T) -> T): T {
    var prev: T
    var next: T
    do {
        prev = value
        next = updater(prev)
    } while (!casValue(prev, next))
    return prev
}

/**
 * Inline copy of [java.util.concurrent.atomic.AtomicReference.updateAndGet].
 */
inline fun <T> MutableProperty<T>.updateAndGet(updater: (old: T) -> T): T {
    var prev: T
    var next: T
    do {
        prev = value
        next = updater(prev)
    } while (!casValue(prev, next))
    return next
}

/**
 * Simplified version of [getAndUpdate] or [updateAndGet].
 */
inline fun <T> MutableProperty<T>.update(updater: (old: T) -> T) {
    var prev: T
    var next: T
    do {
        prev = value
        next = updater(prev)
    } while (!casValue(prev, next))
}
