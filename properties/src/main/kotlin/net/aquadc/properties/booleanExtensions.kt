@file:Suppress("NOTHING_TO_INLINE")
package net.aquadc.properties

inline operator fun Property<Boolean>.not() =
        map { !it }

inline infix fun Property<Boolean>.and(that: Property<Boolean>): Property<Boolean> =
        mapWith(that) { a, b -> a && b }

inline infix fun Property<Boolean>.or(that: Property<Boolean>): Property<Boolean> =
        mapWith(that) { a, b -> a || b }

inline infix fun Property<Boolean>.xor(that: Property<Boolean>): Property<Boolean> =
        mapWith(that) { a, b -> a xor b }
