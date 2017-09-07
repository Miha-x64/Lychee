@file:Suppress("NOTHING_TO_INLINE")
package net.aquadc.properties

import net.aquadc.properties.internal.ConcurrentMultiMappedCachedReferenceProperty


inline fun <T, R> Iterable<Property<T>>.mapValueList(noinline transform: (List<T>) -> R): Property<R> =
        ConcurrentMultiMappedCachedReferenceProperty(this, transform)

inline fun <T, R> Iterable<Property<T>>.foldValues(initial: R, crossinline operation: (acc: R, T) -> R): Property<R> =
        mapValueList {
            var accumulator = initial
            for (element in it) accumulator = operation(accumulator, element)
            accumulator
        }

inline fun <T> Iterable<Property<T>>.firstValueOrNull(crossinline predicate: (T) -> Boolean): Property<T?> =
        mapValueList { it.firstOrNull(predicate) }

inline fun <T> Iterable<Property<T>>.filterValues(crossinline predicate: (T) -> Boolean): Property<List<T>> =
        mapValueList { it.filter(predicate) }

inline fun <T> Iterable<Property<T>>.containsValue(value: T): Property<Boolean> =
        mapValueList { it.contains(value) }

inline fun <T> Iterable<Property<T>>.containsAllValues(values: Collection<T>): Property<Boolean> =
        mapValueList { it.containsAll(values) }

inline fun <T> Iterable<Property<T>>.allValues(crossinline predicate: (T) -> Boolean): Property<Boolean> =
        mapValueList { it.all(predicate) }

inline fun <T> Iterable<Property<T>>.anyValue(crossinline predicate: (T) -> Boolean): Property<Boolean> =
        mapValueList { it.any(predicate) }
