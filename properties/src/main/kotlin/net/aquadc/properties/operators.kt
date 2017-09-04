@file:Suppress("NOTHING_TO_INLINE")
package net.aquadc.properties

import net.aquadc.properties.internal.ConcurrentBiMappedCachedProperty
import kotlin.reflect.KProperty

inline fun <T> Property<T>.readOnlyView() = map { it }

inline fun <A, B, T> Property<A>.mapWith(that: Property<B>, noinline transform: (A, B) -> T): Property<T> =
        ConcurrentBiMappedCachedProperty(this, that, transform)


inline operator fun <T> Property<T>.getValue(thisRef: Any?, property: KProperty<*>): T {
    return value
}

inline operator fun <T> MutableProperty<T>.setValue(thisRef: Any?, property: KProperty<*>, value: T) {
    this.value = value
}
