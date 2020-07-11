@file:[
    JvmName("Incrementals")
    Suppress("UNCHECKED_CAST") // some bad code here
    OptIn(ExperimentalContracts::class)
]
package net.aquadc.persistence.extended.inctemental

import net.aquadc.persistence.extended.EmptyArray
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


@PublishedApi @JvmSynthetic @JvmField internal val empty = RealIncremental(EmptyArray)

@Suppress("NOTHING_TO_INLINE")
inline fun <I : Incremental8<*, *, *, *, *, *, *, *>> emptyIncremental(): I =
    empty as I

inline fun <A, R> Incremental1<A>.mapFold(
    none: ((A) -> Incremental1<A>) -> R,
    one:  (A) -> R
): R = when (values.size) {
    0 -> none(this as (A) -> Incremental1<A>)
    1 ->  one(values[0] as A)
    else -> throw AssertionError()
}

inline fun <A, B, R> Incremental2<A, B>.mapFold(
    none: ((A) -> Incremental2<A, B>) -> R,
    one:  (A, (B) -> Incremental2<A, B>) -> R,
    two:  (A, B) -> R
): R = when (values.size) {
    0 -> none(this as (A) -> Incremental2<A, B>)
    1 ->  one(values[0] as A, this as (B) -> Incremental2<A, B>)
    2 ->  two(values[0] as A, values[1] as B)
    else -> throw AssertionError()
}

inline fun <A, B, C, R> Incremental3<A, B, C>.mapFold(
    none:  ((A) -> Incremental3<A, B, C>) -> R,
    one:   (A, (B) -> Incremental3<A, B, C>) -> R,
    two:   (A, B, (C) -> Incremental3<A, B, C>) -> R,
    three: (A, B, C) -> R
): R {
    contract {
        callsInPlace(none,  InvocationKind.AT_MOST_ONCE)
        callsInPlace(one,   InvocationKind.AT_MOST_ONCE)
        callsInPlace(two,   InvocationKind.AT_MOST_ONCE)
        callsInPlace(three, InvocationKind.AT_MOST_ONCE)
    }
    return when (values.size) {
        0 ->  none(this as (A) -> Incremental3<A, B, C>)
        1 ->   one(values[0] as A, this as (B) -> Incremental3<A, B, C>)
        2 ->   two(values[0] as A, values[1] as B, this as (C) -> Incremental3<A, B, C>)
        3 -> three(values[0] as A, values[1] as B, values[2] as C)
        else -> throw AssertionError()
    }
}

inline fun <A, B, C, D, R> Incremental4<A, B, C, D>.mapFold(
    none:  ((A) -> Incremental4<A, B, C, D>) -> R,
    one:   (A, (B) -> Incremental4<A, B, C, D>) -> R,
    two:   (A, B, (C) -> Incremental4<A, B, C, D>) -> R,
    three: (A, B, C, (D) -> Incremental4<A, B, C, D>) -> R,
    four:  (A, B, C, D) -> R
): R = when (values.size) {
    0 ->  none(this as (A) -> Incremental4<A, B, C, D>)
    1 ->   one(values[0] as A, this as (B) -> Incremental4<A, B, C, D>)
    2 ->   two(values[0] as A, values[1] as B, this as (C) -> Incremental4<A, B, C, D>)
    3 -> three(values[0] as A, values[1] as B, values[2] as C, this as (D) -> Incremental4<A, B, C, D>)
    4 ->  four(values[0] as A, values[1] as B, values[2] as C, values[3] as D)
    else -> throw AssertionError()
}

inline fun <A, B, C, D, E, R> Incremental5<A, B, C, D, E>.mapFold(
    none:  ((A) -> Incremental5<A, B, C, D, E>) -> R,
    one:   (A, (B) -> Incremental5<A, B, C, D, E>) -> R,
    two:   (A, B, (C) -> Incremental5<A, B, C, D, E>) -> R,
    three: (A, B, C, (D) -> Incremental5<A, B, C, D, E>) -> R,
    four:  (A, B, C, D, (E) -> Incremental5<A, B, C, D, E>) -> R,
    five:  (A, B, C, D, E) -> R
): R = when (values.size) {
    0 ->  none(this as (A) -> Incremental5<A, B, C, D, E>)
    1 ->   one(values[0] as A, this as (B) -> Incremental5<A, B, C, D, E>)
    2 ->   two(values[0] as A, values[1] as B, this as (C) -> Incremental5<A, B, C, D, E>)
    3 -> three(values[0] as A, values[1] as B, values[2] as C, this as (D) -> Incremental5<A, B, C, D, E>)
    4 ->  four(values[0] as A, values[1] as B, values[2] as C, values[3] as D, this as (E) -> Incremental5<A, B, C, D, E>)
    5 ->  five(values[0] as A, values[1] as B, values[2] as C, values[3] as D, values[4] as E)
    else -> throw AssertionError()
}

inline fun <A, B, C, D, E, F, R> Incremental6<A, B, C, D, E, F>.mapFold(
    none:  ((A) -> Incremental6<A, B, C, D, E, F>) -> R,
    one:   (A, (B) -> Incremental6<A, B, C, D, E, F>) -> R,
    two:   (A, B, (C) -> Incremental6<A, B, C, D, E, F>) -> R,
    three: (A, B, C, (D) -> Incremental6<A, B, C, D, E, F>) -> R,
    four:  (A, B, C, D, (E) -> Incremental6<A, B, C, D, E, F>) -> R,
    five:  (A, B, C, D, E, (F) -> Incremental6<A, B, C, D, E, F>) -> R,
    six:   (A, B, C, D, E, F) -> R
): R = when (values.size) {
    0 ->  none(this as (A) -> Incremental6<A, B, C, D, E, F>)
    1 ->   one(values[0] as A, this as (B) -> Incremental6<A, B, C, D, E, F>)
    2 ->   two(values[0] as A, values[1] as B, this as (C) -> Incremental6<A, B, C, D, E, F>)
    3 -> three(values[0] as A, values[1] as B, values[2] as C, this as (D) -> Incremental6<A, B, C, D, E, F>)
    4 ->  four(values[0] as A, values[1] as B, values[2] as C, values[3] as D, this as (E) -> Incremental6<A, B, C, D, E, F>)
    5 ->  five(values[0] as A, values[1] as B, values[2] as C, values[3] as D, values[4] as E, this as (F) -> Incremental6<A, B, C, D, E, F>)
    6 ->   six(values[0] as A, values[1] as B, values[2] as C, values[3] as D, values[4] as E, values[5] as F)
    else -> throw AssertionError()
}

inline fun <A, B, C, D, E, F, G, R> Incremental7<A, B, C, D, E, F, G>.mapFold(
    none:  ((A) -> Incremental7<A, B, C, D, E, F, G>) -> R,
    one:   (A, (B) -> Incremental7<A, B, C, D, E, F, G>) -> R,
    two:   (A, B, (C) -> Incremental7<A, B, C, D, E, F, G>) -> R,
    three: (A, B, C, (D) -> Incremental7<A, B, C, D, E, F, G>) -> R,
    four:  (A, B, C, D, (E) -> Incremental7<A, B, C, D, E, F, G>) -> R,
    five:  (A, B, C, D, E, (F) -> Incremental7<A, B, C, D, E, F, G>) -> R,
    six:   (A, B, C, D, E, F, (G) -> Incremental7<A, B, C, D, E, F, G>) -> R,
    seven: (A, B, C, D, E, F, G) -> R
): R = when (values.size) {
    0 -> none(this as (A) -> Incremental7<A, B, C, D, E, F, G>)
    1 ->   one(values[0] as A, this as (B) -> Incremental7<A, B, C, D, E, F, G>)
    2 ->   two(values[0] as A, values[1] as B, this as (C) -> Incremental7<A, B, C, D, E, F, G>)
    3 -> three(values[0] as A, values[1] as B, values[2] as C, this as (D) -> Incremental7<A, B, C, D, E, F, G>)
    4 ->  four(values[0] as A, values[1] as B, values[2] as C, values[3] as D, this as (E) -> Incremental7<A, B, C, D, E, F, G>)
    5 ->  five(values[0] as A, values[1] as B, values[2] as C, values[3] as D, values[4] as E, this as (F) -> Incremental7<A, B, C, D, E, F, G>)
    6 ->   six(values[0] as A, values[1] as B, values[2] as C, values[3] as D, values[4] as E, values[5] as F, this as (G) -> Incremental7<A, B, C, D, E, F, G>)
    7 -> seven(values[0] as A, values[1] as B, values[2] as C, values[3] as D, values[4] as E, values[5] as F, values[6] as G)
    else -> throw AssertionError()
}

inline fun <A, B, C, D, E, F, G, H, R> Incremental8<A, B, C, D, E, F, G, H>.mapFold(
    none:  ((A) -> Incremental8<A, B, C, D, E, F, G, H>) -> R,
    one:   (A, (B) -> Incremental8<A, B, C, D, E, F, G, H>) -> R,
    two:   (A, B, (C) -> Incremental8<A, B, C, D, E, F, G, H>) -> R,
    three: (A, B, C, (D) -> Incremental8<A, B, C, D, E, F, G, H>) -> R,
    four:  (A, B, C, D, (E) -> Incremental8<A, B, C, D, E, F, G, H>) -> R,
    five:  (A, B, C, D, E, (F) -> Incremental8<A, B, C, D, E, F, G, H>) -> R,
    six:   (A, B, C, D, E, F, (G) -> Incremental8<A, B, C, D, E, F, G, H>) -> R,
    seven: (A, B, C, D, E, F, G, (H) -> Incremental8<A, B, C, D, E, F, G, H>) -> R,
    eight: (A, B, C, D, E, F, G, H) -> R
): R = when (values.size) {
    0 ->  none(this as (A) -> Incremental8<A, B, C, D, E, F, G, H>)
    1 ->   one(values[0] as A, this as (B) -> Incremental8<A, B, C, D, E, F, G, H>)
    2 ->   two(values[0] as A, values[1] as B, this as (C) -> Incremental8<A, B, C, D, E, F, G, H>)
    3 -> three(values[0] as A, values[1] as B, values[2] as C, this as (D) -> Incremental8<A, B, C, D, E, F, G, H>)
    4 ->  four(values[0] as A, values[1] as B, values[2] as C, values[3] as D, this as (E) -> Incremental8<A, B, C, D, E, F, G, H>)
    5 ->  five(values[0] as A, values[1] as B, values[2] as C, values[3] as D, values[4] as E, this as (F) -> Incremental8<A, B, C, D, E, F, G, H>)
    6 ->   six(values[0] as A, values[1] as B, values[2] as C, values[3] as D, values[4] as E, values[5] as F, this as (G) -> Incremental8<A, B, C, D, E, F, G, H>)
    7 -> seven(values[0] as A, values[1] as B, values[2] as C, values[3] as D, values[4] as E, values[5] as F, values[6] as G, this as (H) -> Incremental8<A, B, C, D, E, F, G, H>)
    8 -> eight(values[0] as A, values[1] as B, values[2] as C, values[3] as D, values[4] as E, values[5] as F, values[6] as G, values[7] as H)
    else -> throw AssertionError()
}
