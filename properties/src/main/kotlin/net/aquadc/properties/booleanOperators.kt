@file:Suppress("NOTHING_TO_INLINE")
package net.aquadc.properties

import net.aquadc.properties.internal.ConcurrentBiMappedCachedProperty

inline fun Property<Boolean>.not() =
        map { !it }

inline infix fun Property<Boolean>.and(that: Property<Boolean>): Property<Boolean> =
        ConcurrentBiMappedCachedProperty(this, that, { a, b -> a && b })

inline infix fun Property<Boolean>.or(that: Property<Boolean>): Property<Boolean> =
        ConcurrentBiMappedCachedProperty(this, that, { a, b -> a || b })
