@file:JvmName("Either")
@file:Suppress("NOTHING_TO_INLINE")
package net.aquadc.persistence.extended.either

// factory

inline fun <A> EitherLeft(value: A): Either<A, Nothing> =
        Either8.First(value)

inline fun <A> EitherFirst(value: A): Either<A, Nothing> =
        Either8.First(value)

inline fun <B> EitherRight(value: B): Either<Nothing, B> =
        Either8.Second(value)

inline fun <B> EitherSecond(value: B): Either<Nothing, B> =
        Either8.Second(value)

inline fun <C> EitherThird(value: C): Either3<Nothing, Nothing, C> =
        Either8.Third(value)

inline fun <D> EitherFourth(value: D): Either4<Nothing, Nothing, Nothing, D> =
        Either8.Fourth(value)

inline fun <E> EitherFifth(value: E): Either5<Nothing, Nothing, Nothing, Nothing, E> =
        Either8.Fifth(value)

inline fun <F> EitherSixth(value: F): Either6<Nothing, Nothing, Nothing, Nothing, Nothing, F> =
        Either8.Sixth(value)

inline fun <G> EitherSeventh(value: G): Either7<Nothing, Nothing, Nothing, Nothing, Nothing, Nothing, G> =
        Either8.Seventh(value)

inline fun <H> EitherEighth(value: H): Either8<Nothing, Nothing, Nothing, Nothing, Nothing, Nothing, Nothing, H> =
        Either8.Eighth(value)

// map

inline fun <L, R, RR> Either<L, R>.map(
        right: (R) -> RR
): Either<L, RR> =
        when (_which) {
            0 -> this as Either8.First<L>
            1 -> Either8.Second(right(_value as R))
            else -> throw AssertionError()
        }

inline fun <L, R, LR> Either<L, R>.mapLeft(
        left: (L) -> LR
): Either<LR, R> =
        when (_which) {
            0 -> Either8.First(left(_value as L))
            1 -> this as Either8.Second<R>
            else -> throw AssertionError()
        }

inline fun <A, AR, B, BR> Either<A, B>.map(
        left: (A) -> AR, right: (B) -> BR
): Either<AR, BR> =
        when (_which) {
            0 -> Either8.First(left(_value as A))
            1 -> Either8.Second(right(_value as B))
            else -> throw AssertionError()
        }

inline fun <A, AR, B, BR, C, CR> Either3<A, B, C>.map(
        first: (A) -> AR, second: (B) -> BR, third: (C) -> CR
): Either3<AR, BR, CR> =
        when (_which) {
            0 -> Either8.First(first(_value as A))
            1 -> Either8.Second(second(_value as B))
            2 -> Either8.Third(third(_value as C))
            else -> throw AssertionError()
        }

inline fun <A, AR, B, BR, C, CR, D, DR> Either4<A, B, C, D>.map(
        first: (A) -> AR, second: (B) -> BR, third: (C) -> CR, fourth: (D) -> DR
): Either4<AR, BR, CR, DR> =
        when (_which) {
            0 -> Either8.First(first(_value as A))
            1 -> Either8.Second(second(_value as B))
            2 -> Either8.Third(third(_value as C))
            3 -> Either8.Fourth(fourth(_value as D))
            else -> throw AssertionError()
        }

inline fun <A, AR, B, BR, C, CR, D, DR, E, ER> Either5<A, B, C, D, E>.map(
        first: (A) -> AR, second: (B) -> BR, third: (C) -> CR, fourth: (D) -> DR, fifth: (E) -> ER
): Either5<AR, BR, CR, DR, ER> =
        when (_which) {
            0 -> Either8.First(first(_value as A))
            1 -> Either8.Second(second(_value as B))
            2 -> Either8.Third(third(_value as C))
            3 -> Either8.Fourth(fourth(_value as D))
            4 -> Either8.Fifth(fifth(_value as E))
            else -> throw AssertionError()
        }

inline fun <A, AR, B, BR, C, CR, D, DR, E, ER, F, FR> Either6<A, B, C, D, E, F>.map(
        first: (A) -> AR, second: (B) -> BR, third: (C) -> CR, fourth: (D) -> DR, fifth: (E) -> ER, sixth: (F) -> FR
): Either6<AR, BR, CR, DR, ER, FR> =
        when (_which) {
            0 -> Either8.First(first(_value as A))
            1 -> Either8.Second(second(_value as B))
            2 -> Either8.Third(third(_value as C))
            3 -> Either8.Fourth(fourth(_value as D))
            4 -> Either8.Fifth(fifth(_value as E))
            5 -> Either8.Sixth(sixth(_value as F))
            else -> throw AssertionError()
        }

inline fun <A, AR, B, BR, C, CR, D, DR, E, ER, F, FR, G, GR> Either7<A, B, C, D, E, F, G>.map(
        first: (A) -> AR, second: (B) -> BR, third: (C) -> CR, fourth: (D) -> DR,
        fifth: (E) -> ER, sixth: (F) -> FR, seventh: (G) -> GR
): Either7<AR, BR, CR, DR, ER, FR, GR> =
        when (_which) {
            0 -> Either8.First(first(_value as A))
            1 -> Either8.Second(second(_value as B))
            2 -> Either8.Third(third(_value as C))
            3 -> Either8.Fourth(fourth(_value as D))
            4 -> Either8.Fifth(fifth(_value as E))
            5 -> Either8.Sixth(sixth(_value as F))
            6 -> Either8.Seventh(seventh(_value as G))
            else -> throw AssertionError()
        }

inline fun <A, AR, B, BR, C, CR, D, DR, E, ER, F, FR, G, GR, H, HR> Either8<A, B, C, D, E, F, G, H>.map(
        first: (A) -> AR, second: (B) -> BR, third: (C) -> CR, fourth: (D) -> DR,
        fifth: (E) -> ER, sixth: (F) -> FR, seventh: (G) -> GR, eighth: (H) -> HR
): Either8<AR, BR, CR, DR, ER, FR, GR, HR> =
        when (_which) {
            0 -> Either8.First(first(_value as A))
            1 -> Either8.Second(second(_value as B))
            2 -> Either8.Third(third(_value as C))
            3 -> Either8.Fourth(fourth(_value as D))
            4 -> Either8.Fifth(fifth(_value as E))
            5 -> Either8.Sixth(sixth(_value as F))
            6 -> Either8.Seventh(seventh(_value as G))
            7 -> Either8.Eighth(eighth(_value as H))
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
