@file:JvmName("Tuples")
@file:Suppress("NOTHING_TO_INLINE")

package net.aquadc.persistence.extended

import net.aquadc.persistence.struct.PartialStruct
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.struct.StructSnapshot
import net.aquadc.persistence.struct.invoke
import net.aquadc.persistence.type.DataType


// 2

/**
 * A schema consisting of two fields.
 */
class Tuple<A, DA : DataType<A>, B, DB : DataType<B>>(
        firstName: String, firstType: DA,
        secondName: String, secondType: DB
) : Schema<Tuple<A, DA, B, DB>>() {
    @JvmField val First = firstName mut firstType
    @JvmField val Second = secondName mut secondType
}

@Deprecated("renamed", ReplaceWith("this.invoke(first, second)", "net.aquadc.persistence.extended.invoke"))
inline fun <A, DA : DataType<A>, B, DB : DataType<B>>
        Tuple<A, DA, B, DB>.build(
        first: A, second: B
): Struct<Tuple<A, DA, B, DB>> = this(first, second)
/**
 * Creates an instance of a [Tuple] according to [this] schema.
 */
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>>
        Tuple<A, DA, B, DB>.invoke(
        first: A, second: B
): Struct<Tuple<A, DA, B, DB>> =
        this {
            it[First] = first
            it[Second] = second
        }

/**
 * Creates a partial instance of a [Tuple] according to [this] schema.
 */
inline fun <A, DA : DataType<A>, B, DB : DataType<B>>
        Tuple<A, DA, B, DB>.buildPartial(
        first: A? = null, second: B? = null
): PartialStruct<Tuple<A, DA, B, DB>> =
        buildPartial<Tuple<A, DA, B, DB>> {
            if (first != null) it[First] = first
            if (second != null) it[Second] = second
        }

/**
 * Returns first component of [this] [Tuple].
 */
@JvmName("component1of2")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>> Struct<Tuple<A, DA, B, DB>>.component1(): A =
        this[schema.First]

/**
 * Returns second component of [this] [Tuple].
 */
@JvmName("component2of2")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>> Struct<Tuple<A, DA, B, DB>>.component2(): B =
        this[schema.Second]


// 3

/**
 * A schema consisting of three fields.
 */
class Tuple3<A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>>(
        firstName: String, firstType: DA,
        secondName: String, secondType: DB,
        thirdName: String, thirdType: DC
) : Schema<Tuple3<A, DA, B, DB, C, DC>>() {
    @JvmField val First = firstName mut firstType
    @JvmField val Second = secondName mut secondType
    @JvmField val Third = thirdName mut thirdType
}

@Deprecated("renamed", ReplaceWith("this.invoke(first, second, third)", "net.aquadc.persistence.extended.invoke"))
inline fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>>
        Tuple3<A, DA, B, DB, C, DC>.build(
        first: A, second: B, third: C
): Struct<Tuple3<A, DA, B, DB, C, DC>> = this(first, second, third)
/**
 * Creates an instance of a [Tuple3] according to [this] schema.
 */
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>>
        Tuple3<A, DA, B, DB, C, DC>.invoke(
        first: A, second: B, third: C
): StructSnapshot<Tuple3<A, DA, B, DB, C, DC>> =
        this {
            it[First] = first
            it[Second] = second
            it[Third] = third
        }

/**
 * Creates a partial instance of a [Tuple3] according to [this] schema.
 */
inline fun <A : Any, DA : DataType<A>, B : Any, DB : DataType<B>, C : Any, DC : DataType<C>>
        Tuple3<A, DA, B, DB, C, DC>.buildPartial(
        first: A? = null, second: B? = null, third: C? = null
): PartialStruct<Tuple3<A, DA, B, DB, C, DC>> =
        buildPartial<Tuple3<A, DA, B, DB, C, DC>> {
            if (first != null) it[First] = first
            if (second != null) it[Second] = second
            if (third != null) it[Third] = third
        }

/**
 * Returns first component of [this] [Tuple3].
 */
@JvmName("component1of3")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>> Struct<Tuple3<A, DA, B, DB, C, DC>>.component1(): A =
        this[schema.First]

/**
 * Returns second component of [this] [Tuple3].
 */
@JvmName("component2of3")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>> Struct<Tuple3<A, DA, B, DB, C, DC>>.component2(): B =
        this[schema.Second]

/**
 * Returns third component of [this] [Tuple3].
 */
@JvmName("component3of3")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>> Struct<Tuple3<A, DA, B, DB, C, DC>>.component3(): C =
        this[schema.Third]


// 4

/**
 * A schema consisting of four fields.
 */
class Tuple4<A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>>(
        firstName: String, firstType: DA,
        secondName: String, secondType: DB,
        thirdName: String, thirdType: DC,
        fourthName: String, fourthType: DD
) : Schema<Tuple4<A, DA, B, DB, C, DC, D, DD>>() {
    @JvmField val First = firstName mut firstType
    @JvmField val Second = secondName mut secondType
    @JvmField val Third = thirdName mut thirdType
    @JvmField val Fourth = fourthName mut fourthType
}

@Deprecated("renamed", ReplaceWith("this.invoke(first, second, third, fourth)", "net.aquadc.persistence.extended.invoke"))
inline fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>>
        Tuple4<A, DA, B, DB, C, DC, D, DD>.build(
        first: A, second: B, third: C, fourth: D
): Struct<Tuple4<A, DA, B, DB, C, DC, D, DD>> = this(first, second, third, fourth)
/**
 * Creates an instance of a [Tuple4] according to [this] schema.
 */
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>>
        Tuple4<A, DA, B, DB, C, DC, D, DD>.invoke(
        first: A, second: B, third: C, fourth: D
): StructSnapshot<Tuple4<A, DA, B, DB, C, DC, D, DD>> =
        this {
            it[First] = first
            it[Second] = second
            it[Third] = third
            it[Fourth] = fourth
        }

/**
 * Creates a partial instance of a [Tuple4] according to [this] schema.
 */
inline fun <A : Any, DA : DataType<A>, B : Any, DB : DataType<B>, C : Any, DC : DataType<C>, D : Any, DD : DataType<D>>
        Tuple4<A, DA, B, DB, C, DC, D, DD>.buildPartial(
        first: A? = null, second: B? = null, third: C? = null, fourth: D? = null
): PartialStruct<Tuple4<A, DA, B, DB, C, DC, D, DD>> =
        buildPartial<Tuple4<A, DA, B, DB, C, DC, D, DD>> {
            if (first != null) it[First] = first
            if (second != null) it[Second] = second
            if (third != null) it[Third] = third
            if (fourth != null) it[Fourth] = fourth
        }

/**
 * Returns first component of [this] [Tuple4].
 */
@JvmName("component1of4")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>> Struct<Tuple4<A, DA, B, DB, C, DC, D, DD>>.component1(): A =
        this[schema.First]

/**
 * Returns second component of [this] [Tuple4].
 */
@JvmName("component2of4")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>> Struct<Tuple4<A, DA, B, DB, C, DC, D, DD>>.component2(): B =
        this[schema.Second]

/**
 * Returns third component of [this] [Tuple4].
 */
@JvmName("component3of4")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>> Struct<Tuple4<A, DA, B, DB, C, DC, D, DD>>.component3(): C =
        this[schema.Third]

/**
 * Returns fourth component of [this] [Tuple4].
 */
@JvmName("component4of4")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>> Struct<Tuple4<A, DA, B, DB, C, DC, D, DD>>.component4(): D =
        this[schema.Fourth]


// 5

/**
 * A schema consisting of five fields.
 */
class Tuple5<A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>>(
        firstName: String, firstType: DA,
        secondName: String, secondType: DB,
        thirdName: String, thirdType: DC,
        fourthName: String, fourthType: DD,
        fifthName: String, fifthType: DE
) : Schema<Tuple5<A, DA, B, DB, C, DC, D, DD, E, DE>>() {
    @JvmField val First = firstName mut firstType
    @JvmField val Second = secondName mut secondType
    @JvmField val Third = thirdName mut thirdType
    @JvmField val Fourth = fourthName mut fourthType
    @JvmField val Fifth = fifthName mut fifthType
}

@Deprecated("renamed", ReplaceWith("this.invoke(first, second, third, fourth, fifth)", "net.aquadc.persistence.extended.invoke"))
inline fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>>
        Tuple5<A, DA, B, DB, C, DC, D, DD, E, DE>.build(
        first: A, second: B, third: C, fourth: D, fifth: E
): Struct<Tuple5<A, DA, B, DB, C, DC, D, DD, E, DE>> = this(first, second, third, fourth, fifth)
/**
 * Creates an instance of a [Tuple5] according to [this] schema.
 */
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>>
        Tuple5<A, DA, B, DB, C, DC, D, DD, E, DE>.invoke(
        first: A, second: B, third: C, fourth: D, fifth: E
): StructSnapshot<Tuple5<A, DA, B, DB, C, DC, D, DD, E, DE>> =
        this {
            it[First] = first
            it[Second] = second
            it[Third] = third
            it[Fourth] = fourth
            it[Fifth] = fifth
        }

/**
 * Creates a partial instance of a [Tuple5] according to [this] schema.
 */
inline fun <A : Any, DA : DataType<A>, B : Any, DB : DataType<B>, C : Any, DC : DataType<C>, D : Any, DD : DataType<D>, E : Any, DE : DataType<E>>
        Tuple5<A, DA, B, DB, C, DC, D, DD, E, DE>.buildPartial(
        first: A? = null, second: B? = null, third: C? = null, fourth: D? = null, fifth: E? = null
): PartialStruct<Tuple5<A, DA, B, DB, C, DC, D, DD, E, DE>> =
        buildPartial<Tuple5<A, DA, B, DB, C, DC, D, DD, E, DE>> {
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
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>> Struct<Tuple5<A, DA, B, DB, C, DC, D, DD, E, DE>>.component1(): A =
        this[schema.First]

/**
 * Returns second component of [this] [Tuple5].
 */
@JvmName("component2of5")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>> Struct<Tuple5<A, DA, B, DB, C, DC, D, DD, E, DE>>.component2(): B =
        this[schema.Second]

/**
 * Returns third component of [this] [Tuple5].
 */
@JvmName("component3of5")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>> Struct<Tuple5<A, DA, B, DB, C, DC, D, DD, E, DE>>.component3(): C =
        this[schema.Third]

/**
 * Returns fourth component of [this] [Tuple5].
 */
@JvmName("component4of5")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>> Struct<Tuple5<A, DA, B, DB, C, DC, D, DD, E, DE>>.component4(): D =
        this[schema.Fourth]

/**
 * Returns fifth component of [this] [Tuple5].
 */
@JvmName("component5of5")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>> Struct<Tuple5<A, DA, B, DB, C, DC, D, DD, E, DE>>.component5(): E =
        this[schema.Fifth]


// 6

/**
 * A schema consisting of six fields.
 */
class Tuple6<A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>>(
        firstName: String, firstType: DA,
        secondName: String, secondType: DB,
        thirdName: String, thirdType: DC,
        fourthName: String, fourthType: DD,
        fifthName: String, fifthType: DE,
        sixthName: String, sixthType: DF
) : Schema<Tuple6<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF>>() {
    @JvmField val First = firstName mut firstType
    @JvmField val Second = secondName mut secondType
    @JvmField val Third = thirdName mut thirdType
    @JvmField val Fourth = fourthName mut fourthType
    @JvmField val Fifth = fifthName mut fifthType
    @JvmField val Sixth = sixthName mut sixthType
}

@Deprecated("renamed", ReplaceWith("this.invoke(first, second, third, fourth, fifth, sixth)", "net.aquadc.persistence.extended.invoke"))
inline fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>>
        Tuple6<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF>.build(
        first: A, second: B, third: C, fourth: D, fifth: E, sixth: F
): Struct<Tuple6<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF>> = this(first, second, third, fourth, fifth, sixth)
/**
 * Creates an instance of a [Tuple6] according to [this] schema.
 */
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>>
        Tuple6<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF>.invoke(
        first: A, second: B, third: C, fourth: D, fifth: E, sixth: F
): StructSnapshot<Tuple6<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF>> =
        this {
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
inline fun <A : Any, DA : DataType<A>, B : Any, DB : DataType<B>, C : Any, DC : DataType<C>, D : Any, DD : DataType<D>, E : Any, DE : DataType<E>, F : Any, DF : DataType<F>>
        Tuple6<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF>.buildPartial(
        first: A? = null, second: B? = null, third: C? = null, fourth: D? = null, fifth: E? = null, sixth: F? = null
): PartialStruct<Tuple6<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF>> =
        buildPartial<Tuple6<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF>> {
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
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>> Struct<Tuple6<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF>>.component1(): A =
        this[schema.First]

/**
 * Returns second component of [this] [Tuple6].
 */
@JvmName("component2of6")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>> Struct<Tuple6<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF>>.component2(): B =
        this[schema.Second]

/**
 * Returns third component of [this] [Tuple6].
 */
@JvmName("component3of6")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>> Struct<Tuple6<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF>>.component3(): C =
        this[schema.Third]

/**
 * Returns fourth component of [this] [Tuple6].
 */
@JvmName("component4of6")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>> Struct<Tuple6<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF>>.component4(): D =
        this[schema.Fourth]

/**
 * Returns fifth component of [this] [Tuple6].
 */
@JvmName("component5of6")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>> Struct<Tuple6<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF>>.component5(): E =
        this[schema.Fifth]

/**
 * Returns sixth component of [this] [Tuple6].
 */
@JvmName("component6of6")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>> Struct<Tuple6<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF>>.component6(): F =
        this[schema.Sixth]


// 7

/**
 * A schema consisting of seven fields.
 */
class Tuple7<A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>, G, DG : DataType<G>>(
        firstName: String, firstType: DA,
        secondName: String, secondType: DB,
        thirdName: String, thirdType: DC,
        fourthName: String, fourthType: DD,
        fifthName: String, fifthType: DE,
        sixthName: String, sixthType: DF,
        seventhName: String, seventhType: DG
) : Schema<Tuple7<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG>>() {
    @JvmField val First = firstName mut firstType
    @JvmField val Second = secondName mut secondType
    @JvmField val Third = thirdName mut thirdType
    @JvmField val Fourth = fourthName mut fourthType
    @JvmField val Fifth = fifthName mut fifthType
    @JvmField val Sixth = sixthName mut sixthType
    @JvmField val Seventh = seventhName mut seventhType
}

@Deprecated("renamed", ReplaceWith("this.invoke(first, second, third, fourth, fifth, sixth, seventh)", "net.aquadc.persistence.extended.invoke"))
inline fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>, G, DG : DataType<G>>
        Tuple7<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG>.build(
        first: A, second: B, third: C, fourth: D, fifth: E, sixth: F, seventh: G
): Struct<Tuple7<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG>> = this(first, second, third, fourth, fifth, sixth, seventh)
/**
 * Creates an instance of a [Tuple7] according to [this] schema.
 */
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>, G, DG : DataType<G>>
        Tuple7<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG>.invoke(
        first: A, second: B, third: C, fourth: D, fifth: E, sixth: F, seventh: G
): StructSnapshot<Tuple7<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG>> =
        this {
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
inline fun <A : Any, DA : DataType<A>, B : Any, DB : DataType<B>, C : Any, DC : DataType<C>, D : Any, DD : DataType<D>, E : Any, DE : DataType<E>, F : Any, DF : DataType<F>, G : Any, DG : DataType<G>>
        Tuple7<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG>.buildPartial(
        first: A? = null, second: B? = null, third: C? = null, fourth: D? = null, fifth: E? = null, sixth: F? = null, seventh: G? = null
): PartialStruct<Tuple7<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG>> =
        buildPartial<Tuple7<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG>> {
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
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>, G, DG : DataType<G>> Struct<Tuple7<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG>>.component1(): A =
        this[schema.First]

/**
 * Returns second component of [this] [Tuple7].
 */
@JvmName("component2of7")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>, G, DG : DataType<G>> Struct<Tuple7<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG>>.component2(): B =
        this[schema.Second]

/**
 * Returns third component of [this] [Tuple7].
 */
@JvmName("component3of7")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>, G, DG : DataType<G>> Struct<Tuple7<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG>>.component3(): C =
        this[schema.Third]

/**
 * Returns fourth component of [this] [Tuple7].
 */
@JvmName("component4of7")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>, G, DG : DataType<G>> Struct<Tuple7<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG>>.component4(): D =
        this[schema.Fourth]

/**
 * Returns fifth component of [this] [Tuple7].
 */
@JvmName("component5of7")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>, G, DG : DataType<G>> Struct<Tuple7<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG>>.component5(): E =
        this[schema.Fifth]

/**
 * Returns sixth component of [this] [Tuple7].
 */
@JvmName("component6of7")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>, G, DG : DataType<G>> Struct<Tuple7<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG>>.component6(): F =
        this[schema.Sixth]

/**
 * Returns seventh component of [this] [Tuple7].
 */
@JvmName("component7of7")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>, G, DG : DataType<G>> Struct<Tuple7<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG>>.component7(): G =
        this[schema.Seventh]


// 8

/**
 * A schema consisting of eight fields.
 */
class Tuple8<A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>, G, DG : DataType<G>, H, DH : DataType<H>>(
        firstName: String, firstType: DA,
        secondName: String, secondType: DB,
        thirdName: String, thirdType: DC,
        fourthName: String, fourthType: DD,
        fifthName: String, fifthType: DE,
        sixthName: String, sixthType: DF,
        seventhName: String, seventhType: DG,
        eighthName: String, eighthType: DH
) : Schema<Tuple8<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG, H, DH>>() {
    @JvmField val First = firstName mut firstType
    @JvmField val Second = secondName mut secondType
    @JvmField val Third = thirdName mut thirdType
    @JvmField val Fourth = fourthName mut fourthType
    @JvmField val Fifth = fifthName mut fifthType
    @JvmField val Sixth = sixthName mut sixthType
    @JvmField val Seventh = seventhName mut seventhType
    @JvmField val Eighth = eighthName mut eighthType
}

@Deprecated("renamed", ReplaceWith("this.invoke(first, second, third, fourth, fifth, sixth, seventh, eighth)", "net.aquadc.persistence.extended.invoke"))
inline fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>, G, DG : DataType<G>, H, DH : DataType<H>>
        Tuple8<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG, H, DH>.build(
        first: A, second: B, third: C, fourth: D, fifth: E, sixth: F, seventh: G, eighth: H
): Struct<Tuple8<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG, H, DH>> = this(first, second, third, fourth, fifth, sixth, seventh, eighth)
/**
 * Creates an instance of a [Tuple8] according to [this] schema.
 */
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>, G, DG : DataType<G>, H, DH : DataType<H>>
        Tuple8<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG, H, DH>.invoke(
        first: A, second: B, third: C, fourth: D, fifth: E, sixth: F, seventh: G, eighth: H
): StructSnapshot<Tuple8<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG, H, DH>> =
        this {
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
inline fun <A : Any, DA : DataType<A>, B : Any, DB : DataType<B>, C : Any, DC : DataType<C>, D : Any, DD : DataType<D>, E : Any, DE : DataType<E>, F : Any, DF : DataType<F>, G : Any, DG : DataType<G>, H : Any, DH : DataType<H>>
        Tuple8<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG, H, DH>.buildPartial(
        first: A? = null, second: B? = null, third: C? = null, fourth: D? = null, fifth: E? = null, sixth: F? = null, seventh: G? = null, eighth: H? = null
): PartialStruct<Tuple8<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG, H, DH>> =
        buildPartial<Tuple8<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG, H, DH>> {
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
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>, G, DG : DataType<G>, H, DH : DataType<H>> Struct<Tuple8<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG, H, DH>>.component1(): A =
        this[schema.First]

/**
 * Returns second component of [this] [Tuple8].
 */
@JvmName("component2of8")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>, G, DG : DataType<G>, H, DH : DataType<H>> Struct<Tuple8<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG, H, DH>>.component2(): B =
        this[schema.Second]

/**
 * Returns third component of [this] [Tuple8].
 */
@JvmName("component3of8")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>, G, DG : DataType<G>, H, DH : DataType<H>> Struct<Tuple8<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG, H, DH>>.component3(): C =
        this[schema.Third]

/**
 * Returns fourth component of [this] [Tuple8].
 */
@JvmName("component4of8")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>, G, DG : DataType<G>, H, DH : DataType<H>> Struct<Tuple8<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG, H, DH>>.component4(): D =
        this[schema.Fourth]

/**
 * Returns fifth component of [this] [Tuple8].
 */
@JvmName("component5of8")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>, G, DG : DataType<G>, H, DH : DataType<H>> Struct<Tuple8<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG, H, DH>>.component5(): E =
        this[schema.Fifth]

/**
 * Returns sixth component of [this] [Tuple8].
 */
@JvmName("component6of8")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>, G, DG : DataType<G>, H, DH : DataType<H>> Struct<Tuple8<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG, H, DH>>.component6(): F =
        this[schema.Sixth]

/**
 * Returns seventh component of [this] [Tuple8].
 */
@JvmName("component7of8")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>, G, DG : DataType<G>, H, DH : DataType<H>> Struct<Tuple8<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG, H, DH>>.component7(): G =
        this[schema.Seventh]

/**
 * Returns eighth component of [this] [Tuple8].
 */
@JvmName("component8of8")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>, G, DG : DataType<G>, H, DH : DataType<H>> Struct<Tuple8<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG, H, DH>>.component8(): H =
        this[schema.Eighth]
