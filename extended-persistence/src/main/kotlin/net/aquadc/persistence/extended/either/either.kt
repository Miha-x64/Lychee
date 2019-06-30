package net.aquadc.persistence.extended.either

import android.support.annotation.RestrictTo
import net.aquadc.persistence.realHashCode
import net.aquadc.persistence.realToString
import net.aquadc.persistence.reallyEqual


@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
abstract class BaseEither(
        @JvmSynthetic @JvmField internal val _value: Any?,
        @JvmSynthetic @JvmField internal val _which: Int
        // with 4-byte classword, 4-byte OOPS and 8-byte padding `which` field won't increase instance size
        // which will be 16 bytes
) {

    override fun hashCode(): Int =
            ((2 shl _which) - 1) * _value.realHashCode()

    override fun equals(other: Any?): Boolean {
        if (javaClass !== other?.javaClass) return false
        other as BaseEither
        check(_which == other._which) // EitherX class must have _which = X
        return reallyEqual(_value, other._value)
    }

    override fun toString(): String =
            "${javaClass.superclass.simpleName}.$_which(${_value.realToString()})"
            //             EitherN             .   X   (         value          )

}


// 2

sealed class Either<out A, out B>(value: Any?, which: Int) : BaseEither(value, which) {
    class First<A>(value: A) : Either<A, Nothing>(value, 0) {
        val value: A get() = _value as A
    }
    class Second<B>(value: B) : Either<Nothing, B>(value, 1) {
        val value: B get() = _value as B
    }
}


// 3

sealed class Either3<out A, out B, out C>(value: Any?, which: Int) : BaseEither(value, which) {
    class First<out A>(value: A) : Either3<A, Nothing, Nothing>(value, 0) {
        val value: A get() = _value as A
    }
    class Second<out B>(value: B) : Either3<Nothing, B, Nothing>(value, 1) {
        val value: B get() = _value as B
    }
    class Third<out C>(value: C) : Either3<Nothing, Nothing, C>(value, 2) {
        val value: C get() = _value as C
    }
}


// 4

sealed class Either4<out A, out B, out C, out D>(value: Any?, which: Int) : BaseEither(value, which) {
    class First<out A>(value: A) : Either4<A, Nothing, Nothing, Nothing>(value, 0) {
        val value: A get() = _value as A
    }
    class Second<out B>(value: B) : Either4<Nothing, B, Nothing, Nothing>(value, 1) {
        val value: B get() = _value as B
    }
    class Third<out C>(value: C) : Either4<Nothing, Nothing, C, Nothing>(value, 2) {
        val value: C get() = _value as C
    }
    class Fourth<out D>(value: D) : Either4<Nothing, Nothing, Nothing, D>(value, 3) {
        val value: D get() = _value as D
    }
}


// 5

sealed class Either5<out A, out B, out C, out D, out E>(value: Any?, which: Int) : BaseEither(value, which) {
    class First<out A>(value: A) : Either5<A, Nothing, Nothing, Nothing, Nothing>(value, 0) {
        val value: A get() = _value as A
    }
    class Second<out B>(value: B) : Either5<Nothing, B, Nothing, Nothing, Nothing>(value, 1) {
        val value: B get() = _value as B
    }
    class Third<out C>(value: C) : Either5<Nothing, Nothing, C, Nothing, Nothing>(value, 2) {
        val value: C get() = _value as C
    }
    class Fourth<out D>(value: D) : Either5<Nothing, Nothing, Nothing, D, Nothing>(value, 3) {
        val value: D get() = _value as D
    }
    class Fifth<out E>(value: E) : Either5<Nothing, Nothing, Nothing, Nothing, E>(value, 4) {
        val value: E get() = _value as E
    }
}


// 6

sealed class Either6<out A, out B, out C, out D, out E, out F>(value: Any?, which: Int) : BaseEither(value, which) {
    class First<out A>(value: A) : Either6<A, Nothing, Nothing, Nothing, Nothing, Nothing>(value, 0) {
        val value: A get() = _value as A
    }
    class Second<out B>(value: B) : Either6<Nothing, B, Nothing, Nothing, Nothing, Nothing>(value, 1) {
        val value: B get() = _value as B
    }
    class Third<out C>(value: C) : Either6<Nothing, Nothing, C, Nothing, Nothing, Nothing>(value, 2) {
        val value: C get() = _value as C
    }
    class Fourth<out D>(value: D) : Either6<Nothing, Nothing, Nothing, D, Nothing, Nothing>(value, 3) {
        val value: D get() = _value as D
    }
    class Fifth<out E>(value: E) : Either6<Nothing, Nothing, Nothing, Nothing, E, Nothing>(value, 4) {
        val value: E get() = _value as E
    }
    class Sixth<out F>(value: F) : Either6<Nothing, Nothing, Nothing, Nothing, Nothing, F>(value, 5) {
        val value: F get() = _value as F
    }
}


// 7

sealed class Either7<out A, out B, out C, out D, out E, out F, out G>(value: Any?, which: Int) : BaseEither(value, which) {
    class First<out A>(value: A) : Either7<A, Nothing, Nothing, Nothing, Nothing, Nothing, Nothing>(value, 0) {
        val value: A get() = _value as A
    }
    class Second<out B>(value: B) : Either7<Nothing, B, Nothing, Nothing, Nothing, Nothing, Nothing>(value, 1) {
        val value: B get() = _value as B
    }
    class Third<out C>(value: C) : Either7<Nothing, Nothing, C, Nothing, Nothing, Nothing, Nothing>(value, 2) {
        val value: C get() = _value as C
    }
    class Fourth<out D>(value: D) : Either7<Nothing, Nothing, Nothing, D, Nothing, Nothing, Nothing>(value, 3) {
        val value: D get() = _value as D
    }
    class Fifth<out E>(value: E) : Either7<Nothing, Nothing, Nothing, Nothing, E, Nothing, Nothing>(value, 4) {
        val value: E get() = _value as E
    }
    class Sixth<out F>(value: F) : Either7<Nothing, Nothing, Nothing, Nothing, Nothing, F, Nothing>(value, 5) {
        val value: F get() = _value as F
    }
    class Seventh<out G>(value: G) : Either7<Nothing, Nothing, Nothing, Nothing, Nothing, Nothing, G>(value, 6) {
        val value: G get() = _value as G
    }
}


// 8

sealed class Either8<out A, out B, out C, out D, out E, out F, out G, out H>(value: Any?, which: Int) : BaseEither(value, which) {
    class First<out A>(value: A) : Either8<A, Nothing, Nothing, Nothing, Nothing, Nothing, Nothing, Nothing>(value, 0) {
        val value: A get() = _value as A
    }
    class Second<out B>(value: B) : Either8<Nothing, B, Nothing, Nothing, Nothing, Nothing, Nothing, Nothing>(value, 1) {
        val value: B get() = _value as B
    }
    class Third<out C>(value: C) : Either8<Nothing, Nothing, C, Nothing, Nothing, Nothing, Nothing, Nothing>(value, 2) {
        val value: C get() = _value as C
    }
    class Fourth<out D>(value: D) : Either8<Nothing, Nothing, Nothing, D, Nothing, Nothing, Nothing, Nothing>(value, 3) {
        val value: D get() = _value as D
    }
    class Fifth<out E>(value: E) : Either8<Nothing, Nothing, Nothing, Nothing, E, Nothing, Nothing, Nothing>(value, 4) {
        val value: E get() = _value as E
    }
    class Sixth<out F>(value: F) : Either8<Nothing, Nothing, Nothing, Nothing, Nothing, F, Nothing, Nothing>(value, 5) {
        val value: F get() = _value as F
    }
    class Seventh<out G>(value: G) : Either8<Nothing, Nothing, Nothing, Nothing, Nothing, Nothing, G, Nothing>(value, 6) {
        val value: G get() = _value as G
    }
    class Eighth<out H>(value: H) : Either8<Nothing, Nothing, Nothing, Nothing, Nothing, Nothing, Nothing, H>(value, 7) {
        val value: H get() = _value as H
    }
}
