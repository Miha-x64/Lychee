package net.aquadc.persistence.extended.inctemental

import net.aquadc.persistence.realHashCode
import net.aquadc.persistence.realToString
import net.aquadc.persistence.reallyEqual


abstract class Incremental1<A> internal constructor(value: Array<Any?>)
    : Incremental2<A, Nothing>(value)

abstract class Incremental2<A, B> internal constructor(value: Array<Any?>)
    : Incremental3<A, B, Nothing>(value)

abstract class Incremental3<A, B, C> internal constructor(value: Array<Any?>)
    : Incremental4<A, B, C, Nothing>(value)

abstract class Incremental4<A, B, C, D> internal constructor(value: Array<Any?>)
    : Incremental5<A, B, C, D, Nothing>(value)

abstract class Incremental5<A, B, C, D, E> internal constructor(value: Array<Any?>)
    : Incremental6<A, B, C, D, E, Nothing>(value)

abstract class Incremental6<A, B, C, D, E, F> internal constructor(value: Array<Any?>)
    : Incremental7<A, B, C, D, E, F, Nothing>(value)

abstract class Incremental7<A, B, C, D, E, F, G> internal constructor(value: Array<Any?>)
    : Incremental8<A, B, C, D, E, F, G, Nothing>(value)

abstract class Incremental8<A, B, C, D, E, F, G, H> internal constructor(
    @[PublishedApi JvmSynthetic JvmField] internal val values: Array<Any?>
) {

    final override fun hashCode(): Int =
        values.realHashCode()

    final override fun equals(other: Any?): Boolean =
        javaClass === other?.javaClass && reallyEqual(values, (other as Incremental8<*, *, *, *, *, *, *, *>).values)

    final override fun toString(): String =
        "Incremental${values.realToString("(", ")")}"
}

internal class RealIncremental internal constructor(values: Array<Any?>)
    : Incremental1<Nothing>(values), (Any?) -> RealIncremental {

    override fun invoke(p1: Any?): RealIncremental =
        RealIncremental(values + p1)
}
