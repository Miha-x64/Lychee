@file:JvmName("Tuples")
@file:Suppress("NOTHING_TO_INLINE")

package net.aquadc.persistence.extended

import net.aquadc.persistence.struct.PartialStruct
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.struct.build
import net.aquadc.persistence.type.DataType


// 2

/**
 * A schema consisting of two fields.
 */
class Tuple<A, B>(
        firstName: String, firstType: DataType<A>,
        secondName: String, secondType: DataType<B>
) : Schema<Tuple<A, B>>() {
    @JvmField val First = firstName mut firstType
    @JvmField val Second = secondName mut secondType
}

/**
 * Creates an instance of a [Tuple] according to [this] schema.
 */
inline fun <A, B> Tuple<A, B>.build(
        first: A, second: B
): Struct<Tuple<A, B>> =
        build {
            it[First] = first
            it[Second] = second
        }

/**
 * Creates a partial instance of a [Tuple] according to [this] schema.
 */
inline fun <A : Any, B : Any> Tuple<A, B>.buildPartial(
        first: A? = null, second: B? = null
): PartialStruct<Tuple<A, B>> =
        buildPartial<Tuple<A, B>> {
            if (first != null) it[First] = first
            if (second != null) it[Second] = second
        }

/**
 * Returns first component of [this] [Tuple].
 */
@JvmName("component1of2")
inline operator fun <A, B> Struct<Tuple<A, B>>.component1(): A =
        this[schema.First]

/**
 * Returns second component of [this] [Tuple].
 */
@JvmName("component2of2")
inline operator fun <A, B> Struct<Tuple<A, B>>.component2(): B =
        this[schema.Second]


// 3

/**
 * A schema consisting of three fields.
 */
class Tuple3<A, B, C>(
        firstName: String, firstType: DataType<A>,
        secondName: String, secondType: DataType<B>,
        thirdName: String, thirdType: DataType<C>
) : Schema<Tuple3<A, B, C>>() {
    @JvmField val First = firstName mut firstType
    @JvmField val Second = secondName mut secondType
    @JvmField val Third = thirdName mut thirdType
}

/**
 * Creates an instance of a [Tuple3] according to [this] schema.
 */
inline fun <A, B, C> Tuple3<A, B, C>.build(
        first: A, second: B, third: C
): Struct<Tuple3<A, B, C>> =
        build {
            it[First] = first
            it[Second] = second
            it[Third] = third
        }

/**
 * Creates a partial instance of a [Tuple3] according to [this] schema.
 */
inline fun <A : Any, B : Any, C : Any> Tuple3<A, B, C>.buildPartial(
        first: A? = null, second: B? = null, third: C? = null
): PartialStruct<Tuple3<A, B, C>> =
        buildPartial<Tuple3<A, B, C>> {
            if (first != null) it[First] = first
            if (second != null) it[Second] = second
            if (third != null) it[Third] = third
        }

/**
 * Returns first component of [this] [Tuple3].
 */
@JvmName("component1of3")
inline operator fun <A, B, C> Struct<Tuple3<A, B, C>>.component1(): A =
        this[schema.First]

/**
 * Returns second component of [this] [Tuple3].
 */
@JvmName("component2of3")
inline operator fun <A, B, C> Struct<Tuple3<A, B, C>>.component2(): B =
        this[schema.Second]

/**
 * Returns third component of [this] [Tuple3].
 */
@JvmName("component3of3")
inline operator fun <A, B, C> Struct<Tuple3<A, B, C>>.component3(): C =
        this[schema.Third]


// 4

/**
 * A schema consisting of four fields.
 */
class Tuple4<A, B, C, D>(
        firstName: String, firstType: DataType<A>,
        secondName: String, secondType: DataType<B>,
        thirdName: String, thirdType: DataType<C>,
        fourthName: String, fourthType: DataType<D>
) : Schema<Tuple4<A, B, C, D>>() {
    @JvmField val First = firstName mut firstType
    @JvmField val Second = secondName mut secondType
    @JvmField val Third = thirdName mut thirdType
    @JvmField val Fourth = fourthName mut fourthType
}

/**
 * Creates an instance of a [Tuple4] according to [this] schema.
 */
inline fun <A, B, C, D> Tuple4<A, B, C, D>.build(
        first: A, second: B, third: C, fourth: D
): Struct<Tuple4<A, B, C, D>> =
        build {
            it[First] = first
            it[Second] = second
            it[Third] = third
            it[Fourth] = fourth
        }

/**
 * Creates a partial instance of a [Tuple4] according to [this] schema.
 */
inline fun <A : Any, B : Any, C : Any, D : Any> Tuple4<A, B, C, D>.buildPartial(
        first: A? = null, second: B? = null, third: C? = null, fourth: D? = null
): PartialStruct<Tuple4<A, B, C, D>> =
        buildPartial<Tuple4<A, B, C, D>> {
            if (first != null) it[First] = first
            if (second != null) it[Second] = second
            if (third != null) it[Third] = third
            if (fourth != null) it[Fourth] = fourth
        }

/**
 * Returns first component of [this] [Tuple4].
 */
@JvmName("component1of4")
inline operator fun <A, B, C, D> Struct<Tuple4<A, B, C, D>>.component1(): A =
        this[schema.First]

/**
 * Returns second component of [this] [Tuple4].
 */
@JvmName("component2of4")
inline operator fun <A, B, C, D> Struct<Tuple4<A, B, C, D>>.component2(): B =
        this[schema.Second]

/**
 * Returns third component of [this] [Tuple4].
 */
@JvmName("component3of4")
inline operator fun <A, B, C, D> Struct<Tuple4<A, B, C, D>>.component3(): C =
        this[schema.Third]

/**
 * Returns fourth component of [this] [Tuple4].
 */
@JvmName("component4of4")
inline operator fun <A, B, C, D> Struct<Tuple4<A, B, C, D>>.component4(): D =
        this[schema.Fourth]


// 5

/**
 * A schema consisting of five fields.
 */
class Tuple5<A, B, C, D, E>(
        firstName: String, firstType: DataType<A>,
        secondName: String, secondType: DataType<B>,
        thirdName: String, thirdType: DataType<C>,
        fourthName: String, fourthType: DataType<D>,
        fifthName: String, fifthType: DataType<E>
) : Schema<Tuple5<A, B, C, D, E>>() {
    @JvmField val First = firstName mut firstType
    @JvmField val Second = secondName mut secondType
    @JvmField val Third = thirdName mut thirdType
    @JvmField val Fourth = fourthName mut fourthType
    @JvmField val Fifth = fifthName mut fifthType
}

/**
 * Creates an instance of a [Tuple5] according to [this] schema.
 */
inline fun <A, B, C, D, E> Tuple5<A, B, C, D, E>.build(
        first: A, second: B, third: C, fourth: D, fifth: E
): Struct<Tuple5<A, B, C, D, E>> =
        build {
            it[First] = first
            it[Second] = second
            it[Third] = third
            it[Fourth] = fourth
            it[Fifth] = fifth
        }

/**
 * Creates a partial instance of a [Tuple5] according to [this] schema.
 */
inline fun <A : Any, B : Any, C : Any, D : Any, E : Any> Tuple5<A, B, C, D, E>.buildPartial(
        first: A? = null, second: B? = null, third: C? = null, fourth: D? = null, fifth: E? = null
): PartialStruct<Tuple5<A, B, C, D, E>> =
        buildPartial<Tuple5<A, B, C, D, E>> {
            if (first != null) it[First] = first
            if (second != null) it[Second] = second
            if (third != null) it[Third] = third
            if (fourth != null) it[Fourth] = fourth
            if (fifth != null) it[Fifth] = fifth
        }

/**
 * Returns first component of [this] [Tuple5].
 */
@JvmName("component1of5")
inline operator fun <A, B, C, D, E> Struct<Tuple5<A, B, C, D, E>>.component1(): A =
        this[schema.First]

/**
 * Returns second component of [this] [Tuple5].
 */
@JvmName("component2of5")
inline operator fun <A, B, C, D, E> Struct<Tuple5<A, B, C, D, E>>.component2(): B =
        this[schema.Second]

/**
 * Returns third component of [this] [Tuple5].
 */
@JvmName("component3of5")
inline operator fun <A, B, C, D, E> Struct<Tuple5<A, B, C, D, E>>.component3(): C =
        this[schema.Third]

/**
 * Returns fourth component of [this] [Tuple5].
 */
@JvmName("component4of5")
inline operator fun <A, B, C, D, E> Struct<Tuple5<A, B, C, D, E>>.component4(): D =
        this[schema.Fourth]

/**
 * Returns fifth component of [this] [Tuple5].
 */
@JvmName("component5of5")
inline operator fun <A, B, C, D, E> Struct<Tuple5<A, B, C, D, E>>.component5(): E =
        this[schema.Fifth]


// 6

/**
 * A schema consisting of six fields.
 */
class Tuple6<A, B, C, D, E, F>(
        firstName: String, firstType: DataType<A>,
        secondName: String, secondType: DataType<B>,
        thirdName: String, thirdType: DataType<C>,
        fourthName: String, fourthType: DataType<D>,
        fifthName: String, fifthType: DataType<E>,
        sixthName: String, sixthType: DataType<F>
) : Schema<Tuple6<A, B, C, D, E, F>>() {
    @JvmField val First = firstName mut firstType
    @JvmField val Second = secondName mut secondType
    @JvmField val Third = thirdName mut thirdType
    @JvmField val Fourth = fourthName mut fourthType
    @JvmField val Fifth = fifthName mut fifthType
    @JvmField val Sixth = sixthName mut sixthType
}

/**
 * Creates an instance of a [Tuple6] according to [this] schema.
 */
inline fun <A, B, C, D, E, F> Tuple6<A, B, C, D, E, F>.build(
        first: A, second: B, third: C, fourth: D, fifth: E, sixth: F
): Struct<Tuple6<A, B, C, D, E, F>> =
        build {
            it[First] = first
            it[Second] = second
            it[Third] = third
            it[Fourth] = fourth
            it[Fifth] = fifth
            it[Sixth] = sixth
        }

/**
 * Creates a partial instance of a [Tuple6] according to [this] schema.
 */
inline fun <A : Any, B : Any, C : Any, D : Any, E : Any, F : Any> Tuple6<A, B, C, D, E, F>.buildPartial(
        first: A? = null, second: B? = null, third: C? = null, fourth: D? = null, fifth: E? = null, sixth: F? = null
): PartialStruct<Tuple6<A, B, C, D, E, F>> =
        buildPartial<Tuple6<A, B, C, D, E, F>> {
            if (first != null) it[First] = first
            if (second != null) it[Second] = second
            if (third != null) it[Third] = third
            if (fourth != null) it[Fourth] = fourth
            if (fifth != null) it[Fifth] = fifth
            if (sixth != null) it[Sixth] = sixth
        }

/**
 * Returns first component of [this] [Tuple6].
 */
@JvmName("component1of6")
inline operator fun <A, B, C, D, E, F> Struct<Tuple6<A, B, C, D, E, F>>.component1(): A =
        this[schema.First]

/**
 * Returns second component of [this] [Tuple6].
 */
@JvmName("component2of6")
inline operator fun <A, B, C, D, E, F> Struct<Tuple6<A, B, C, D, E, F>>.component2(): B =
        this[schema.Second]

/**
 * Returns third component of [this] [Tuple6].
 */
@JvmName("component3of6")
inline operator fun <A, B, C, D, E, F> Struct<Tuple6<A, B, C, D, E, F>>.component3(): C =
        this[schema.Third]

/**
 * Returns fourth component of [this] [Tuple6].
 */
@JvmName("component4of6")
inline operator fun <A, B, C, D, E, F> Struct<Tuple6<A, B, C, D, E, F>>.component4(): D =
        this[schema.Fourth]

/**
 * Returns fifth component of [this] [Tuple6].
 */
@JvmName("component5of6")
inline operator fun <A, B, C, D, E, F> Struct<Tuple6<A, B, C, D, E, F>>.component5(): E =
        this[schema.Fifth]

/**
 * Returns sixth component of [this] [Tuple6].
 */
@JvmName("component6of6")
inline operator fun <A, B, C, D, E, F> Struct<Tuple6<A, B, C, D, E, F>>.component6(): F =
        this[schema.Sixth]


// 7

/**
 * A schema consisting of seven fields.
 */
class Tuple7<A, B, C, D, E, F, G>(
        firstName: String, firstType: DataType<A>,
        secondName: String, secondType: DataType<B>,
        thirdName: String, thirdType: DataType<C>,
        fourthName: String, fourthType: DataType<D>,
        fifthName: String, fifthType: DataType<E>,
        sixthName: String, sixthType: DataType<F>,
        seventhName: String, seventhType: DataType<G>
) : Schema<Tuple7<A, B, C, D, E, F, G>>() {
    @JvmField val First = firstName mut firstType
    @JvmField val Second = secondName mut secondType
    @JvmField val Third = thirdName mut thirdType
    @JvmField val Fourth = fourthName mut fourthType
    @JvmField val Fifth = fifthName mut fifthType
    @JvmField val Sixth = sixthName mut sixthType
    @JvmField val Seventh = seventhName mut seventhType
}

/**
 * Creates an instance of a [Tuple7] according to [this] schema.
 */
inline fun <A, B, C, D, E, F, G> Tuple7<A, B, C, D, E, F, G>.build(
        first: A, second: B, third: C, fourth: D, fifth: E, sixth: F, seventh: G
): Struct<Tuple7<A, B, C, D, E, F, G>> =
        build {
            it[First] = first
            it[Second] = second
            it[Third] = third
            it[Fourth] = fourth
            it[Fifth] = fifth
            it[Sixth] = sixth
            it[Seventh] = seventh
        }

/**
 * Creates a partial instance of a [Tuple7] according to [this] schema.
 */
inline fun <A : Any, B : Any, C : Any, D : Any, E : Any, F : Any, G : Any> Tuple7<A, B, C, D, E, F, G>.buildPartial(
        first: A? = null, second: B? = null, third: C? = null, fourth: D? = null, fifth: E? = null, sixth: F? = null, seventh: G? = null
): PartialStruct<Tuple7<A, B, C, D, E, F, G>> =
        buildPartial<Tuple7<A, B, C, D, E, F, G>> {
            if (first != null) it[First] = first
            if (second != null) it[Second] = second
            if (third != null) it[Third] = third
            if (fourth != null) it[Fourth] = fourth
            if (fifth != null) it[Fifth] = fifth
            if (sixth != null) it[Sixth] = sixth
            if (seventh != null) it[Seventh] = seventh
        }

/**
 * Returns first component of [this] [Tuple7].
 */
@JvmName("component1of7")
inline operator fun <A, B, C, D, E, F, G> Struct<Tuple7<A, B, C, D, E, F, G>>.component1(): A =
        this[schema.First]

/**
 * Returns second component of [this] [Tuple7].
 */
@JvmName("component2of7")
inline operator fun <A, B, C, D, E, F, G> Struct<Tuple7<A, B, C, D, E, F, G>>.component2(): B =
        this[schema.Second]

/**
 * Returns third component of [this] [Tuple7].
 */
@JvmName("component3of7")
inline operator fun <A, B, C, D, E, F, G> Struct<Tuple7<A, B, C, D, E, F, G>>.component3(): C =
        this[schema.Third]

/**
 * Returns fourth component of [this] [Tuple7].
 */
@JvmName("component4of7")
inline operator fun <A, B, C, D, E, F, G> Struct<Tuple7<A, B, C, D, E, F, G>>.component4(): D =
        this[schema.Fourth]

/**
 * Returns fifth component of [this] [Tuple7].
 */
@JvmName("component5of7")
inline operator fun <A, B, C, D, E, F, G> Struct<Tuple7<A, B, C, D, E, F, G>>.component5(): E =
        this[schema.Fifth]

/**
 * Returns sixth component of [this] [Tuple7].
 */
@JvmName("component6of7")
inline operator fun <A, B, C, D, E, F, G> Struct<Tuple7<A, B, C, D, E, F, G>>.component6(): F =
        this[schema.Sixth]

/**
 * Returns seventh component of [this] [Tuple7].
 */
@JvmName("component7of7")
inline operator fun <A, B, C, D, E, F, G> Struct<Tuple7<A, B, C, D, E, F, G>>.component7(): G =
        this[schema.Seventh]


// 8

/**
 * A schema consisting of eight fields.
 */
class Tuple8<A, B, C, D, E, F, G, H>(
        firstName: String, firstType: DataType<A>,
        secondName: String, secondType: DataType<B>,
        thirdName: String, thirdType: DataType<C>,
        fourthName: String, fourthType: DataType<D>,
        fifthName: String, fifthType: DataType<E>,
        sixthName: String, sixthType: DataType<F>,
        seventhName: String, seventhType: DataType<G>,
        eighthName: String, eighthType: DataType<H>
) : Schema<Tuple8<A, B, C, D, E, F, G, H>>() {
    @JvmField val First = firstName mut firstType
    @JvmField val Second = secondName mut secondType
    @JvmField val Third = thirdName mut thirdType
    @JvmField val Fourth = fourthName mut fourthType
    @JvmField val Fifth = fifthName mut fifthType
    @JvmField val Sixth = sixthName mut sixthType
    @JvmField val Seventh = seventhName mut seventhType
    @JvmField val Eighth = eighthName mut eighthType
}

/**
 * Creates an instance of a [Tuple8] according to [this] schema.
 */
inline fun <A, B, C, D, E, F, G, H> Tuple8<A, B, C, D, E, F, G, H>.build(
        first: A, second: B, third: C, fourth: D, fifth: E, sixth: F, seventh: G, eighth: H
): Struct<Tuple8<A, B, C, D, E, F, G, H>> =
        build {
            it[First] = first
            it[Second] = second
            it[Third] = third
            it[Fourth] = fourth
            it[Fifth] = fifth
            it[Sixth] = sixth
            it[Seventh] = seventh
            it[Eighth] = eighth
        }

/**
 * Creates a partial instance of a [Tuple8] according to [this] schema.
 */
inline fun <A : Any, B : Any, C : Any, D : Any, E : Any, F : Any, G : Any, H : Any> Tuple8<A, B, C, D, E, F, G, H>.buildPartial(
        first: A? = null, second: B? = null, third: C? = null, fourth: D? = null, fifth: E? = null, sixth: F? = null, seventh: G? = null, eighth: H? = null
): PartialStruct<Tuple8<A, B, C, D, E, F, G, H>> =
        buildPartial<Tuple8<A, B, C, D, E, F, G, H>> {
            if (first != null) it[First] = first
            if (second != null) it[Second] = second
            if (third != null) it[Third] = third
            if (fourth != null) it[Fourth] = fourth
            if (fifth != null) it[Fifth] = fifth
            if (sixth != null) it[Sixth] = sixth
            if (seventh != null) it[Seventh] = seventh
            if (eighth != null) it[Eighth] = eighth
        }

/**
 * Returns first component of [this] [Tuple8].
 */
@JvmName("component1of8")
inline operator fun <A, B, C, D, E, F, G, H> Struct<Tuple8<A, B, C, D, E, F, G, H>>.component1(): A =
        this[schema.First]

/**
 * Returns second component of [this] [Tuple8].
 */
@JvmName("component2of8")
inline operator fun <A, B, C, D, E, F, G, H> Struct<Tuple8<A, B, C, D, E, F, G, H>>.component2(): B =
        this[schema.Second]

/**
 * Returns third component of [this] [Tuple8].
 */
@JvmName("component3of8")
inline operator fun <A, B, C, D, E, F, G, H> Struct<Tuple8<A, B, C, D, E, F, G, H>>.component3(): C =
        this[schema.Third]

/**
 * Returns fourth component of [this] [Tuple8].
 */
@JvmName("component4of8")
inline operator fun <A, B, C, D, E, F, G, H> Struct<Tuple8<A, B, C, D, E, F, G, H>>.component4(): D =
        this[schema.Fourth]

/**
 * Returns fifth component of [this] [Tuple8].
 */
@JvmName("component5of8")
inline operator fun <A, B, C, D, E, F, G, H> Struct<Tuple8<A, B, C, D, E, F, G, H>>.component5(): E =
        this[schema.Fifth]

/**
 * Returns sixth component of [this] [Tuple8].
 */
@JvmName("component6of8")
inline operator fun <A, B, C, D, E, F, G, H> Struct<Tuple8<A, B, C, D, E, F, G, H>>.component6(): F =
        this[schema.Sixth]

/**
 * Returns seventh component of [this] [Tuple8].
 */
@JvmName("component7of8")
inline operator fun <A, B, C, D, E, F, G, H> Struct<Tuple8<A, B, C, D, E, F, G, H>>.component7(): G =
        this[schema.Seventh]

/**
 * Returns eighth component of [this] [Tuple8].
 */
@JvmName("component8of8")
inline operator fun <A, B, C, D, E, F, G, H> Struct<Tuple8<A, B, C, D, E, F, G, H>>.component8(): H =
        this[schema.Eighth]
