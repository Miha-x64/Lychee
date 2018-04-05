@file:Suppress("NOTHING_TO_INLINE")
package net.aquadc.properties

import net.aquadc.properties.internal.ConcMultiMappedProperty
import net.aquadc.properties.internal.UnsMultiMappedProperty

/**
 * Maps a list of properties into a single [Property].
 */
inline fun <T, R> Iterable<Property<T>>.mapValueList(noinline transform: (List<T>) -> R): Property<R> =
        if (all { it.isConcurrent }) ConcMultiMappedProperty(this, transform)
        else UnsMultiMappedProperty(this, transform)

/**
 * Folds a list of properties into a single [Property].
 */
inline fun <T, R> Iterable<Property<T>>.foldValues(initial: R, crossinline operation: (acc: R, T) -> R): Property<R> =
        mapValueList {
            var accumulator = initial
            for (element in it) accumulator = operation(accumulator, element)
            accumulator
        }

/**
 * Returns a property which holds first non-null property's value, or null, if all are nulls.
 */
inline fun <T> Iterable<Property<T>>.firstValueOrNull(crossinline predicate: (T) -> Boolean): Property<T?> =
        mapValueList { it.firstOrNull(predicate) }

/**
 * Returns a property which holds a filtered collection.
 */
inline fun <T> Iterable<Property<T>>.filterValues(crossinline predicate: (T) -> Boolean): Property<List<T>> =
        mapValueList { it.filter(predicate) }

/**
 * Returns a property which holds `true` if [this] contains a property holding [value].
 */
inline fun <T> Iterable<Property<T>>.containsValue(value: T): Property<Boolean> =
        mapValueList(ContainsValue(value))

/**
 * Returns a property which holds `true` if [this] contains a property holding all [values].
 */
inline fun <T> Iterable<Property<T>>.containsAllValues(values: Collection<T>): Property<Boolean> =
        mapValueList(ContainsAll(values))

/**
 * Returns a property which holds `true` if all properties' values in [this] are conforming to [predicate].
 */
inline fun <T> Iterable<Property<T>>.allValues(crossinline predicate: (T) -> Boolean): Property<Boolean> =
        mapValueList { it.all(predicate) }

/**
 * Returns a property which holds `true` if any of properties' values in [this] conforms to [predicate].
 */
inline fun <T> Iterable<Property<T>>.anyValue(crossinline predicate: (T) -> Boolean): Property<Boolean> =
        mapValueList { it.any(predicate) }

// if we'd just use lambdas, they'd be copied into call-site

@PublishedApi internal class ContainsValue<in T>(
        private val value: T
) : (List<T>) -> Boolean {
    override fun invoke(p1: List<T>): Boolean = p1.contains(value)
}

@PublishedApi internal class ContainsAll<in T>(
        private val values: Collection<T>
) : (List<T>) -> Boolean {
    override fun invoke(p1: List<T>): Boolean = p1.containsAll(values)
}
