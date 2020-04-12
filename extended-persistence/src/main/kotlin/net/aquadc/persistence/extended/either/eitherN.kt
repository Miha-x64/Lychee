package net.aquadc.persistence.extended.either

import net.aquadc.persistence.realHashCode
import net.aquadc.persistence.realToString
import net.aquadc.persistence.reallyEqual


typealias Either<A, B> = Either8<A, B, Nothing, Nothing, Nothing, Nothing, Nothing, Nothing>

typealias Either3<A, B, C> = Either8<A, B, C, Nothing, Nothing, Nothing, Nothing, Nothing>

typealias Either4<A, B, C, D> = Either8<A, B, C, D, Nothing, Nothing, Nothing, Nothing>

typealias Either5<A, B, C, D, E> = Either8<A, B, C, D, E, Nothing, Nothing, Nothing>

typealias Either6<A, B, C, D, E, F> = Either8<A, B, C, D, E, F, Nothing, Nothing>

typealias Either7<A, B, C, D, E, F, G> = Either8<A, B, C, D, E, F, G, Nothing>

sealed class Either8<out A, out B, out C, out D, out E, out F, out G, out H>(
        @JvmSynthetic @JvmField internal val _value: Any?,
        @JvmSynthetic @JvmField internal val _which: Int
        // with 4-byte classword, 4-byte OOPS and 8-byte padding `which` field won't increase instance size
        // which will be 16 bytes
) {
    @Deprecated("will be reworked without sealed classes, use .map and .fold")
    class First<out A>(value: A) : Either8<A, Nothing, Nothing, Nothing, Nothing, Nothing, Nothing, Nothing>(value, 0) {
        val value: A get() = _value as A
    }
    @Deprecated("will be reworked without sealed classes, use .map and .fold")
    class Second<out B>(value: B) : Either8<Nothing, B, Nothing, Nothing, Nothing, Nothing, Nothing, Nothing>(value, 1) {
        val value: B get() = _value as B
    }
    @Deprecated("will be reworked without sealed classes, use .map and .fold")
    class Third<out C>(value: C) : Either8<Nothing, Nothing, C, Nothing, Nothing, Nothing, Nothing, Nothing>(value, 2) {
        val value: C get() = _value as C
    }
    @Deprecated("will be reworked without sealed classes, use .map and .fold")
    class Fourth<out D>(value: D) : Either8<Nothing, Nothing, Nothing, D, Nothing, Nothing, Nothing, Nothing>(value, 3) {
        val value: D get() = _value as D
    }
    @Deprecated("will be reworked without sealed classes, use .map and .fold")
    class Fifth<out E>(value: E) : Either8<Nothing, Nothing, Nothing, Nothing, E, Nothing, Nothing, Nothing>(value, 4) {
        val value: E get() = _value as E
    }
    @Deprecated("will be reworked without sealed classes, use .map and .fold")
    class Sixth<out F>(value: F) : Either8<Nothing, Nothing, Nothing, Nothing, Nothing, F, Nothing, Nothing>(value, 5) {
        val value: F get() = _value as F
    }
    @Deprecated("will be reworked without sealed classes, use .map and .fold")
    class Seventh<out G>(value: G) : Either8<Nothing, Nothing, Nothing, Nothing, Nothing, Nothing, G, Nothing>(value, 6) {
        val value: G get() = _value as G
    }
    @Deprecated("will be reworked without sealed classes, use .map and .fold")
    class Eighth<out H>(value: H) : Either8<Nothing, Nothing, Nothing, Nothing, Nothing, Nothing, Nothing, H>(value, 7) {
        val value: H get() = _value as H
    }

    override fun hashCode(): Int =
            ((2 shl _which) - 1) * _value.realHashCode()

    override fun equals(other: Any?): Boolean {
        if (javaClass !== other?.javaClass) return false
        other as Either8<*, *, *, *, *, *, *, *>
        return _which == other._which && reallyEqual(_value, other._value)
    }

    override fun toString(): String =
            "Either.$_which(${_value.realToString()})"
}

// nested types are not visible through typealiases
@Deprecated("will be reworked without sealed classes, use .map and .fold")
typealias EitherLeft<A> = Either8.First<A>

@Deprecated("will be reworked without sealed classes, use .map and .fold")
typealias EitherRight<B> = Either8.Second<B>

@Deprecated("will be reworked without sealed classes, use .map and .fold")
typealias EitherThird<C> = Either8.Third<C>

@Deprecated("will be reworked without sealed classes, use .map and .fold")
typealias EitherFourth<D> = Either8.Fourth<D>

@Deprecated("will be reworked without sealed classes, use .map and .fold")
typealias EitherFifth<E> = Either8.Fifth<E>

@Deprecated("will be reworked without sealed classes, use .map and .fold")
typealias EitherSixth<F> = Either8.Sixth<F>

@Deprecated("will be reworked without sealed classes, use .map and .fold")
typealias EitherSeventh<G> = Either8.Seventh<G>

@Deprecated("will be reworked without sealed classes, use .map and .fold")
typealias EitherEighth<H> = Either8.Eighth<H>
