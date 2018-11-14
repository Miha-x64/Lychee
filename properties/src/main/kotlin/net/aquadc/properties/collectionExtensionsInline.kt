@file:Suppress("NOTHING_TO_INLINE")
@file:JvmName("CollectionPropertiesInline")
package net.aquadc.properties

import net.aquadc.properties.function.Collectionz
import net.aquadc.properties.function.`AppliedFunc1-`
import net.aquadc.properties.function.identity
import net.aquadc.properties.internal.`MultiMapped-`


/**
 * Maps a list of properties into a single [Property] by transforming a [List] of their values.
 */
inline fun <T, R> Collection<Property<T>>.mapValueList(noinline transform: (List<T>) -> R): Property<R> =
        `MultiMapped-`(this, transform)

/**
 * Maps a list of properties into a single [Property] containing a [List] of their values.
 */
inline fun <T> Collection<Property<T>>.valueList(): Property<List<T>> =
        `MultiMapped-`(this, identity())

/**
 * Folds a list of properties into a single [Property].
 */
@Suppress("UNCHECKED_CAST")
inline fun <T, R> Collection<Property<T>>.foldValues(initial: R, crossinline operation: (acc: R, T) -> R): Property<R> =
        mapValueList({ it: Any ->
            it as List<T>

            var accumulator = initial
            for (element in it) accumulator = operation(accumulator, element)
            val ret: Any? = accumulator
            ret
        } as (List<T>) -> R)

/**
 * Returns a property which holds first non-null property's value, or null, if all are nulls.
 */
@Suppress("UNCHECKED_CAST")
inline fun <T> Collection<Property<T>>.firstValueOrNull(crossinline predicate: (T) -> Boolean): Property<T?> =
        mapValueList({ it: Any ->
            it as List<T>

            val ret: Any? = it.firstOrNull(predicate)
            ret
        } as (List<T>) -> T?)

/**
 * Returns a property which holds a filtered collection.
 */
@Suppress("UNCHECKED_CAST")
inline fun <T> Collection<Property<T>>.filterValues(crossinline predicate: (T) -> Boolean): Property<List<T>> =
        mapValueList({ it: Any ->
            it as List<T>

            val ret: Any? = it.filter(predicate)
            ret
        } as (List<T>) -> List<T>)

/**
 * Returns a property which holds `true` if [this] contains a property holding [value].
 */
@Suppress("UNCHECKED_CAST")
inline fun <T> Collection<Property<T>>.containsValue(value: T): Property<Boolean> =
        mapValueList(`AppliedFunc1-`(value, 2) as (Collection<T>) -> Boolean)

/**
 * Returns a property which holds `true` if [this] contains a property holding all [values].
 */
@Suppress("UNCHECKED_CAST")
inline fun <T> Collection<Property<T>>.containsAllValues(values: Collection<T>): Property<Boolean> =
        mapValueList(`AppliedFunc1-`(values, 1) as (Collection<T>) -> Boolean)

/**
 * Returns a property which holds `true` if all properties' values in [this] are conforming to [predicate].
 */
@Suppress("UNCHECKED_CAST")
inline fun <T> Collection<Property<T>>.allValues(crossinline predicate: (T) -> Boolean): Property<Boolean> =
        mapValueList({ it: Any ->
            it as List<T>

            val ret: Any? = it.all(predicate)
            ret
        } as (List<T>) -> Boolean)

/**
 * Returns a property which holds `true` if any of properties' values in [this] conforms to [predicate].
 */
@Suppress("UNCHECKED_CAST")
inline fun <T> Collection<Property<T>>.anyValue(crossinline predicate: (T) -> Boolean): Property<Boolean> =
        mapValueList({ it: Any ->
            @Suppress("UNCHECKED_CAST")
            it as List<T>

            val ret: Any? = it.any(predicate)
            ret
        } as (List<T>) -> Boolean)

/**
 * Returns a [Property] which is `true` when [this.value].[Collection.isEmpty].
 */
@Deprecated("Was generalized.", ReplaceWith("map(Collectionz.Empty)",
        "net.aquadc.properties.map", "net.aquadc.properties.function.Collectionz"))
inline fun Property<Collection<*>?>.isEmpty(): Property<Boolean> =
        map(Collectionz.Empty)

/**
 * Returns a [Property] which is `true` when [this.value].[Collection.isNotEmpty].
 */
@Deprecated("Was generalized.", ReplaceWith("map(Collectionz.NotEmpty)",
        "net.aquadc.properties.map", "net.aquadc.properties.function.Collectionz"))
inline fun Property<Collection<*>?>.isNotEmpty(): Property<Boolean> =
        map(Collectionz.NotEmpty)
