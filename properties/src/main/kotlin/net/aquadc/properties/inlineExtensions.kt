@file:Suppress("NOTHING_TO_INLINE")
@file:JvmName("PropertiesInline")
package net.aquadc.properties

import android.os.Looper
import javafx.application.Platform
import net.aquadc.properties.internal.*
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.TimeUnit
import kotlin.reflect.KProperty

/**
 * Observer for [Property]<T>.
 */
typealias ChangeListener<T> = (old: T, new: T) -> Unit
// typealias just takes place in metadata, let it live with inline functions


//
// Boolean
//

/**
 * Returns inverted view on this property.
 */
inline operator fun Property<Boolean>.not(): Property<Boolean> =
        map(UnaryNotBinaryAnd)

/**
 * Returns a view on [this] && [that].
 */
inline infix fun Property<Boolean>.and(that: Property<Boolean>): Property<Boolean> =
        mapWith(that, UnaryNotBinaryAnd)

/**
 * Returns a view on [this] || [that].
 */
inline infix fun Property<Boolean>.or(that: Property<Boolean>): Property<Boolean> =
        mapWith(that, Or)

/**
 * Returns a view on [this] ^ [that].
 */
inline infix fun Property<Boolean>.xor(that: Property<Boolean>): Property<Boolean> =
        mapWith(that, Xor)

/**
 * Sets [this] value to `true`.
 */
inline fun MutableProperty<Boolean>.set() { value = true }


/**
 * Sets [this] value to `false`.
 */
inline fun MutableProperty<Boolean>.clear() { value = false }

/**
 * Every time property becomes set (`true`),
 * it will be unset (`false`) and [action] will be performed.
 */
inline fun MutableProperty<Boolean>.clearEachAnd(crossinline action: () -> Unit): Unit =
        addChangeListener { wasSet, isSet ->
            if (!wasSet && isSet) {
                value = false
                action()
            }
        }


//
// CharSequence
//

inline val Property<CharSequence>.length get() = map(CharSeqLength)

inline val Property<CharSequence>.isEmpty get() = map(CharSeqBooleanFunc.Empty)
inline val Property<CharSequence>.isNotEmpty get() = map(CharSeqBooleanFunc.NotEmpty)

inline val Property<CharSequence>.isBlank get() = map(CharSeqBooleanFunc.Blank)
inline val Property<CharSequence>.isNotBlank get() = map(CharSeqBooleanFunc.NotBlank)

inline val Property<CharSequence>.trimmed get() = map(TrimmedCharSeq)


//
// common
//

@Suppress("UNCHECKED_CAST")
inline fun <T> Property<T>.readOnlyView() = map(Just as (T) -> T)

inline fun <T> Property<T>.distinct(noinline areEqual: (T, T) -> Boolean) =
        if (this.mayChange) DistinctPropertyWrapper(this, areEqual) else this

/**
 * Returns a debounced wrapper around this property.
 * Will work only on threads which can accept tasks:
 * * JavaFX Application thread (via [Platform]);
 * * Android [Looper] thread;
 * * a thread of a [ForkJoinPool].
 *
 * Single-threaded debounced wrapper will throw exception when created on inappropriate thread.
 * Concurrent debounced wrapper will throw exception when listener gets subscribed from inappropriate thread.
 */
inline fun <T> Property<T>.debounced(delay: Long, unit: TimeUnit) =
        if (mayChange) DebouncedProperty(this, delay, unit)
        else this

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

//
// factories
//

inline fun <T> mutablePropertyOf(value: T, concurrent: Boolean): MutableProperty<T> =
        if (concurrent) ConcMutableProperty(value)
        else UnsMutableProperty(value)

inline fun <T> concurrentMutablePropertyOf(value: T): MutableProperty<T> =
        ConcMutableProperty(value)

inline fun <T> unsynchronizedMutablePropertyOf(value: T): MutableProperty<T> =
        UnsMutableProperty(value)

inline fun immutablePropertyOf(value: Boolean): Property<Boolean> = when (value) {
    true -> ImmutableReferenceProperty.TRUE
    false -> ImmutableReferenceProperty.FALSE
}

inline fun <T> immutablePropertyOf(value: T): Property<T> =
        ImmutableReferenceProperty(value)


//
// bulk
//


/**
 * Maps a list of properties into a single [Property].
 */
inline fun <T, R> Collection<Property<T>>.mapValueList(noinline transform: (List<T>) -> R): Property<R> =
        if (all { it.isConcurrent }) MultiMappedProperty(this, transform)
        else MultiMappedProperty(this, transform)

/**
 * Folds a list of properties into a single [Property].
 */
inline fun <T, R> Collection<Property<T>>.foldValues(initial: R, crossinline operation: (acc: R, T) -> R): Property<R> =
        mapValueList {
            var accumulator = initial
            for (element in it) accumulator = operation(accumulator, element)
            accumulator
        }

/**
 * Returns a property which holds first non-null property's value, or null, if all are nulls.
 */
inline fun <T> Collection<Property<T>>.firstValueOrNull(crossinline predicate: (T) -> Boolean): Property<T?> =
        mapValueList { it.firstOrNull(predicate) }

/**
 * Returns a property which holds a filtered collection.
 */
inline fun <T> Collection<Property<T>>.filterValues(crossinline predicate: (T) -> Boolean): Property<List<T>> =
        mapValueList { it.filter(predicate) }

/**
 * Returns a property which holds `true` if [this] contains a property holding [value].
 */
inline fun <T> Collection<Property<T>>.containsValue(value: T): Property<Boolean> =
        mapValueList(ContainsValue(value))

/**
 * Returns a property which holds `true` if [this] contains a property holding all [values].
 */
inline fun <T> Collection<Property<T>>.containsAllValues(values: Collection<T>): Property<Boolean> =
        mapValueList(ContainsAll(values))

/**
 * Returns a property which holds `true` if all properties' values in [this] are conforming to [predicate].
 */
inline fun <T> Collection<Property<T>>.allValues(crossinline predicate: (T) -> Boolean): Property<Boolean> =
        mapValueList { it.all(predicate) }

/**
 * Returns a property which holds `true` if any of properties' values in [this] conforms to [predicate].
 */
inline fun <T> Collection<Property<T>>.anyValue(crossinline predicate: (T) -> Boolean): Property<Boolean> =
        mapValueList { it.any(predicate) }


//
// Delegates
//

inline operator fun <T> Property<T>.getValue(thisRef: Any?, property: KProperty<*>): T {
    return value
}

inline operator fun <T> MutableProperty<T>.setValue(thisRef: Any?, property: KProperty<*>, value: T) {
    this.value = value
}
