@file:JvmName("Eithers")
@file:Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
package net.aquadc.persistence.extended.either

// factory

inline fun <A, B> EitherLeft(value: A): Either<A, B> =
    RealEither(value, 0)

inline fun <A, B> EitherFirst(value: A): Either<A, B> =
    RealEither(value, 0)

inline fun <A, B> EitherRight(value: B): Either<A, B> =
    RealEither(value, 1)

inline fun <A, B> EitherSecond(value: B): Either<A, B> =
    RealEither(value, 1)

inline fun <A, B, C> EitherThird(value: C): Either3<A, B, C> =
    RealEither(value, 2)

inline fun <A, B, C, D> EitherFourth(value: D): Either4<A, B, C, D> =
    RealEither(value, 3)

inline fun <A, B, C, D, E> EitherFifth(value: E): Either5<A, B, C, D, E> =
    RealEither(value, 4)

inline fun <A, B, C, D, E, F> EitherSixth(value: F): Either6<A, B, C, D, E, F> =
    RealEither(value, 5)

inline fun <A, B, C, D, E, F, G> EitherSeventh(value: G): Either7<A, B, C, D, E, F, G> =
    RealEither(value, 6)

inline fun <A, B, C, D, E, F, G, H> EitherEighth(value: H): Either8<A, B, C, D, E, F, G, H> =
    RealEither(value, 7)

// map

inline fun <L, R, RR> Either<L, R>.map(
    right: (R) -> RR
): Either<L, RR> =
    when (_which) {
        0 -> this as RealEither
        1 -> RealEither(right(_value as R), 1)
        else -> throw AssertionError()
    }

inline fun <L, R, LR> Either<L, R>.mapLeft(
    left: (L) -> LR
): Either<LR, R> =
    when (_which) {
        0 -> RealEither(left(_value as L), 0)
        1 -> this as RealEither
        else -> throw AssertionError()
    }

inline fun <A, AR, B, BR> Either<A, B>.map(
    left: (A) -> AR, right: (B) -> BR
): Either<AR, BR> =
    when (_which) {
        0 -> RealEither(left(_value as A), 0)
        1 -> RealEither(right(_value as B), 1)
        else -> throw AssertionError()
    }

inline fun <A, AR, B, BR, C, CR> Either3<A, B, C>.map(
    first: (A) -> AR, second: (B) -> BR, third: (C) -> CR
): Either3<AR, BR, CR> =
    when (_which) {
        0 -> RealEither(first(_value as A), 0)
        1 -> RealEither(second(_value as B), 1)
        2 -> RealEither(third(_value as C), 2)
        else -> throw AssertionError()
    }

inline fun <A, AR, B, BR, C, CR, D, DR> Either4<A, B, C, D>.map(
    first: (A) -> AR, second: (B) -> BR, third: (C) -> CR, fourth: (D) -> DR
): Either4<AR, BR, CR, DR> =
    when (_which) {
        0 -> RealEither(first(_value as A), 0)
        1 -> RealEither(second(_value as B), 1)
        2 -> RealEither(third(_value as C), 2)
        3 -> RealEither(fourth(_value as D), 3)
        else -> throw AssertionError()
    }

inline fun <A, AR, B, BR, C, CR, D, DR, E, ER> Either5<A, B, C, D, E>.map(
    first: (A) -> AR, second: (B) -> BR, third: (C) -> CR, fourth: (D) -> DR, fifth: (E) -> ER
): Either5<AR, BR, CR, DR, ER> =
    when (_which) {
        0 -> RealEither(first(_value as A), 0)
        1 -> RealEither(second(_value as B), 1)
        2 -> RealEither(third(_value as C), 2)
        3 -> RealEither(fourth(_value as D), 3)
        4 -> RealEither(fifth(_value as E), 4)
        else -> throw AssertionError()
    }

inline fun <A, AR, B, BR, C, CR, D, DR, E, ER, F, FR> Either6<A, B, C, D, E, F>.map(
    first: (A) -> AR, second: (B) -> BR, third: (C) -> CR, fourth: (D) -> DR, fifth: (E) -> ER, sixth: (F) -> FR
): Either6<AR, BR, CR, DR, ER, FR> =
    when (_which) {
        0 -> RealEither(first(_value as A), 0)
        1 -> RealEither(second(_value as B), 1)
        2 -> RealEither(third(_value as C), 2)
        3 -> RealEither(fourth(_value as D), 3)
        4 -> RealEither(fifth(_value as E), 4)
        5 -> RealEither(sixth(_value as F), 5)
        else -> throw AssertionError()
    }

inline fun <A, AR, B, BR, C, CR, D, DR, E, ER, F, FR, G, GR> Either7<A, B, C, D, E, F, G>.map(
    first: (A) -> AR, second: (B) -> BR, third: (C) -> CR, fourth: (D) -> DR,
    fifth: (E) -> ER, sixth: (F) -> FR, seventh: (G) -> GR
): Either7<AR, BR, CR, DR, ER, FR, GR> =
    when (_which) {
        0 -> RealEither(first(_value as A), 0)
        1 -> RealEither(second(_value as B), 1)
        2 -> RealEither(third(_value as C), 2)
        3 -> RealEither(fourth(_value as D), 3)
        4 -> RealEither(fifth(_value as E), 4)
        5 -> RealEither(sixth(_value as F), 5)
        6 -> RealEither(seventh(_value as G), 6)
        else -> throw AssertionError()
    }

inline fun <A, AR, B, BR, C, CR, D, DR, E, ER, F, FR, G, GR, H, HR> Either8<A, B, C, D, E, F, G, H>.map(
    first: (A) -> AR, second: (B) -> BR, third: (C) -> CR, fourth: (D) -> DR,
    fifth: (E) -> ER, sixth: (F) -> FR, seventh: (G) -> GR, eighth: (H) -> HR
): Either8<AR, BR, CR, DR, ER, FR, GR, HR> =
    when (_which) {
        0 -> RealEither(first(_value as A), 0)
        1 -> RealEither(second(_value as B), 1)
        2 -> RealEither(third(_value as C), 2)
        3 -> RealEither(fourth(_value as D), 3)
        4 -> RealEither(fifth(_value as E), 4)
        5 -> RealEither(sixth(_value as F), 5)
        6 -> RealEither(seventh(_value as G), 6)
        7 -> RealEither(eighth(_value as H), 7)
        else -> throw AssertionError()
    }

// fold

inline fun <A, B, R> Either<A, B>.fold(
        left: (A) -> R, right: (B) -> R
): R =
        when (_which) {
            0 -> left(_value as A)
            1 -> right(_value as B)
            else -> throw AssertionError()
        }

inline fun <A, B, C, R> Either3<A, B, C>.fold(
        first: (A) -> R, second: (B) -> R, third: (C) -> R
): R =
        when (_which) {
            0 -> first(_value as A)
            1 -> second(_value as B)
            2 -> third(_value as C)
            else -> throw AssertionError()
        }

inline fun <A, B, C, D, R> Either4<A, B, C, D>.fold(
        first: (A) -> R, second: (B) -> R, third: (C) -> R, fourth: (D) -> R
): R =
        when (_which) {
            0 -> first(_value as A)
            1 -> second(_value as B)
            2 -> third(_value as C)
            3 -> fourth(_value as D)
            else -> throw AssertionError()
        }

inline fun <A, B, C, D, E, R> Either5<A, B, C, D, E>.fold(
        first: (A) -> R, second: (B) -> R, third: (C) -> R, fourth: (D) -> R, fifth: (E) -> R
): R =
        when (_which) {
            0 -> first(_value as A)
            1 -> second(_value as B)
            2 -> third(_value as C)
            3 -> fourth(_value as D)
            4 -> fifth(_value as E)
            else -> throw AssertionError()
        }

inline fun <A, B, C, D, E, F, R> Either6<A, B, C, D, E, F>.fold(
        first: (A) -> R, second: (B) -> R, third: (C) -> R, fourth: (D) -> R, fifth: (E) -> R, sixth: (F) -> R
): R =
        when (_which) {
            0 -> first(_value as A)
            1 -> second(_value as B)
            2 -> third(_value as C)
            3 -> fourth(_value as D)
            4 -> fifth(_value as E)
            5 -> sixth(_value as F)
            else -> throw AssertionError()
        }

inline fun <A, B, C, D, E, F, G, R> Either7<A, B, C, D, E, F, G>.fold(
        first: (A) -> R, second: (B) -> R, third: (C) -> R, fourth: (D) -> R,
        fifth: (E) -> R, sixth: (F) -> R, seventh: (G) -> R
): R =
        when (_which) {
            0 -> first(_value as A)
            1 -> second(_value as B)
            2 -> third(_value as C)
            3 -> fourth(_value as D)
            4 -> fifth(_value as E)
            5 -> sixth(_value as F)
            6 -> seventh(_value as G)
            else -> throw AssertionError()
        }

inline fun <A, B, C, D, E, F, G, H, R> Either8<A, B, C, D, E, F, G, H>.fold(
        first: (A) -> R, second: (B) -> R, third: (C) -> R, fourth: (D) -> R,
        fifth: (E) -> R, sixth: (F) -> R, seventh: (G) -> R, eighth: (H) -> R
): R =
        when (_which) {
            0 -> first(_value as A)
            1 -> second(_value as B)
            2 -> third(_value as C)
            3 -> fourth(_value as D)
            4 -> fifth(_value as E)
            5 -> sixth(_value as F)
            6 -> seventh(_value as G)
            7 -> eighth(_value as H)
            else -> throw AssertionError()
        }

// other

fun <A, B> Either<A, B>.unwrap(message: String? = null): B =
    when (_which) {
        0 -> throw NoSuchElementException(message)
        1 -> _value as B
        else -> throw AssertionError()
    }

@JvmName("unwrapWithCause")
fun <A : Throwable, B> Either<A, B>.unwrap(message: String? = null): B =
    when (_which) {
        0 -> throw IllegalStateException(message, _value as A)
        // unfortunately, there's no NoSuchElementException(message, cause)
        1 -> _value as B
        else -> throw AssertionError()
    }
