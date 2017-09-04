@file:Suppress("NOTHING_TO_INLINE")
package net.aquadc.properties

inline fun <T, R> Property<T>.map(noinline transform: (T) -> R): Property<R> =
        MappedProperty<T, R>(this, transform)

inline fun <T> Property<T>.readOnlyView() = map { it }

inline fun <A, B, T> Property<A>.mapWith(that: Property<B>, noinline transform: (A, B) -> T): Property<T> =
        ConcurrentBiMappedCachedProperty(this, that, transform)
