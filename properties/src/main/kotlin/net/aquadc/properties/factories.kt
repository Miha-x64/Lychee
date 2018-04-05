@file:Suppress("NOTHING_TO_INLINE")

package net.aquadc.properties

import net.aquadc.properties.internal.ConcMutableProperty
import net.aquadc.properties.internal.ImmutableReferenceProperty
import net.aquadc.properties.internal.UnsMutableProperty


inline fun <T> mutablePropertyOf(value: T, concurrent: Boolean): MutableProperty<T> =
        if (concurrent) ConcMutableProperty(value)
        else UnsMutableProperty(value)

inline fun <T> concurrentMutablePropertyOf(value: T): MutableProperty<T> =
        ConcMutableProperty(value)

inline fun <T> unsynchronizedMutablePropertyOf(value: T): MutableProperty<T> =
        UnsMutableProperty(value)

@PublishedApi @JvmField internal val immutableTrue = ImmutableReferenceProperty(true)
@PublishedApi @JvmField internal val immutableFalse = ImmutableReferenceProperty(false)

inline fun immutablePropertyOf(value: Boolean): Property<Boolean> = when (value) {
    true -> immutableTrue
    false -> immutableFalse
}

inline fun <T> immutablePropertyOf(value: T): Property<T> =
        ImmutableReferenceProperty(value)
