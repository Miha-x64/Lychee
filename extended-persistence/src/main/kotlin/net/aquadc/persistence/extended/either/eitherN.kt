package net.aquadc.persistence.extended.either

import net.aquadc.persistence.realHashCode
import net.aquadc.persistence.realToString
import net.aquadc.persistence.reallyEqual


// I'd like to use `typealias`es but they are invisible from Java, as well as Nothing type.
// all following classes are candidates for class merging optimization (ProGuard, R8)

abstract class Either<out A, out B> internal constructor(value: Any?, which: Int)
    : Either3<A, B, Nothing>(value, which)

abstract class Either3<out A, out B, out C> internal constructor(value: Any?, which: Int)
    : Either4<A, B, C, Nothing>(value, which)

abstract class Either4<out A, out B, out C, out D> internal constructor(value: Any?, which: Int)
    : Either5<A, B, C, D, Nothing>(value, which)

abstract class Either5<out A, out B, out C, out D, out E> internal constructor(value: Any?, which: Int)
    : Either6<A, B, C, D, E, Nothing>(value, which)

abstract class Either6<out A, out B, out C, out D, out E, out F> internal constructor(value: Any?, which: Int)
    : Either7<A, B, C, D, E, F, Nothing>(value, which)

abstract class Either7<out A, out B, out C, out D, out E, out F, out G> internal constructor(value: Any?, which: Int)
    : Either8<A, B, C, D, E, F, G, Nothing>(value, which)

abstract class Either8<out A, out B, out C, out D, out E, out F, out G, out H> internal constructor(
    @JvmSynthetic @JvmField @PublishedApi internal val _value: Any?,
    @JvmSynthetic @JvmField @PublishedApi internal val _which: Int // 0-based
    // with 4-byte classword, 4-byte OOPS, and 8-byte padding `which` field won't increase instance size
    // which will be 16 bytes
) {
    final override fun hashCode(): Int =
        ((2 shl _which) - 1) * _value.realHashCode()

    final override fun equals(other: Any?): Boolean =
        javaClass === other?.javaClass && (other as Either8<*, *, *, *, *, *, *, *>)
            .let { _which == it._which && reallyEqual(_value, it._value) }

    final override fun toString(): String =
        "Either.$_which(${_value.realToString()})"
}

@PublishedApi internal class RealEither(value: Any?, which: Int)
    : Either<Nothing, Nothing>(value, which)
