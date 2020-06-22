@file:JvmName("Tuples")
@file:Suppress("NOTHING_TO_INLINE")
package net.aquadc.persistence.extended

import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.FieldSet
import net.aquadc.persistence.struct.PartialStruct
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.struct.StructSnapshot
import net.aquadc.persistence.struct.emptyFieldSet
import net.aquadc.persistence.struct.invoke
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.nothing


// 0

/** Always an empty object. */
@JvmField val unit: DataType.NotNull.Partial<Unit, Box<Nothing, DataType.NotNull.Simple<Nothing>>> =
    object : DataType.NotNull.Partial<Unit, Box<Nothing, DataType.NotNull.Simple<Nothing>>>(Box("unused", nothing)) {
        override fun load(
            fields: FieldSet<Box<Nothing, Simple<Nothing>>, FieldDef<Box<Nothing, Simple<Nothing>>, *, *>>, values: Any?
        ): Unit = Unit
        override fun fields(value: Unit): FieldSet<Box<Nothing, Simple<Nothing>>, FieldDef<Box<Nothing, Simple<Nothing>>, *, *>> =
            emptyFieldSet()
        override fun store(value: Unit): Any? =
            null
    }


// 1

class Box<A, DA : DataType<A>>(
    firstName: String, firstType: DA
) : Schema<Box<A, DA>>() {
    @JvmField val First = firstName mut firstType
}

/** Creates an instance of a [Box] according to [this] schema. */
@JvmSynthetic // returns Object[], useless for Java
inline operator fun <A, DA : DataType<A>>
    Box<A, DA>.invoke(
    first: A
): StructSnapshot<Box<A, DA>> =
    this {
        it[First] = first
    }

// Java version of Box.invoke()
@JvmName("newBox")
fun <A, DA : DataType<A>>
    newBoxBoxed(
    type: Box<A, DA>,
    first: A
): Struct<Box<A, DA>> =
    type(first)

/*looks useless* Creates a partial instance of a [Tuple] according to [this] schema. */
/*inline fun <A : Any, DA : DataType<A>>
    Box<A, DA>.buildPartial(
    first: A? = null
): PartialStruct<Box<A, DA>> =
    buildPartial<Box<A, DA>> {
        if (first != null) it[First] = first
    }*/

/** Returns first component of [this] [Tuple]. */
@JvmName("firstOf1")
inline operator fun <A, DA : DataType<A>> Struct<Box<A, DA>>.component1(): A =
    this[schema.First]


// 2

/** A schema consisting of two fields. */
class Tuple<A, DA : DataType<A>, B, DB : DataType<B>>(
        firstName: String, firstType: DA,
        secondName: String, secondType: DB
) : Schema<Tuple<A, DA, B, DB>>() {
    @JvmField val First = firstName mut firstType
    @JvmField val Second = secondName mut secondType
}

/** A shortcut for [Tuple] with default field names. */
@JvmSynthetic // useless for Java
operator fun <A, DA : DataType<A>, B, DB : DataType<B>>
        DA.times(second: DB): Tuple<A, DA, B, DB> =
        Tuple("first", this, "second", second)

/** Creates an instance of a [Tuple] according to [this] schema. */
@JvmSynthetic // returns Object[], useless for Java
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>>
        Tuple<A, DA, B, DB>.invoke(
        first: A, second: B
): StructSnapshot<Tuple<A, DA, B, DB>> =
        this {
            it[First] = first
            it[Second] = second
        }

@JvmName("newTuple")
fun <A, DA : DataType<A>, B, DB : DataType<B>>
    newTupleBoxed(
    type: Tuple<A, DA, B, DB>,
    first: A, second: B
): Struct<Tuple<A, DA, B, DB>> =
    type(first, second)

/** Creates a partial instance of a [Tuple] according to [this] schema. */
inline fun <A : Any, DA : DataType<A>, B : Any, DB : DataType<B>>
        Tuple<A, DA, B, DB>.buildPartial(
        first: A? = null, second: B? = null
): PartialStruct<Tuple<A, DA, B, DB>> =
        buildPartial<Tuple<A, DA, B, DB>> {
            if (first != null) it[First] = first
            if (second != null) it[Second] = second
        }

/** Returns first component of [this] [Tuple]. */
@JvmName("firstOf2")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>> Struct<Tuple<A, DA, B, DB>>.component1(): A =
        this[schema.First]

/** Returns second component of [this] [Tuple]. */
@JvmName("secondOf2")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>> Struct<Tuple<A, DA, B, DB>>.component2(): B =
        this[schema.Second]


// 3

/** A schema consisting of three fields. */
class Tuple3<A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>>(
        firstName: String, firstType: DA,
        secondName: String, secondType: DB,
        thirdName: String, thirdType: DC
) : Schema<Tuple3<A, DA, B, DB, C, DC>>() {
    @JvmField val First = firstName mut firstType
    @JvmField val Second = secondName mut secondType
    @JvmField val Third = thirdName mut thirdType
}

/** A shortcut for [Tuple3] with default field names. */
@JvmSynthetic // useless for Java
operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>>
        Tuple<A, DA, B, DB>.times(third: DC): Tuple3<A, DA, B, DB, C, DC> =
        Tuple3("first", First.type, "second", Second.type, "third", third)

/** Creates an instance of a [Tuple3] according to [this] schema. */
@JvmSynthetic // returns Object[], useless for Java
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>>
        Tuple3<A, DA, B, DB, C, DC>.invoke(
        first: A, second: B, third: C
): StructSnapshot<Tuple3<A, DA, B, DB, C, DC>> =
        this {
            it[First] = first
            it[Second] = second
            it[Third] = third
        }

@JvmName("newTuple")
fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>>
    newTupleBoxed(
    type: Tuple3<A, DA, B, DB, C, DC>,
    first: A, second: B, third: C
): Struct<Tuple3<A, DA, B, DB, C, DC>> =
    type(first, second, third)

/** Creates a partial instance of a [Tuple3] according to [this] schema. */
inline fun <A : Any, DA : DataType<A>, B : Any, DB : DataType<B>, C : Any, DC : DataType<C>>
        Tuple3<A, DA, B, DB, C, DC>.buildPartial(
        first: A? = null, second: B? = null, third: C? = null
): PartialStruct<Tuple3<A, DA, B, DB, C, DC>> =
        buildPartial<Tuple3<A, DA, B, DB, C, DC>> {
            if (first != null) it[First] = first
            if (second != null) it[Second] = second
            if (third != null) it[Third] = third
        }

/** Returns first component of [this] [Tuple3]. */
@JvmName("firstOf3")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>> Struct<Tuple3<A, DA, B, DB, C, DC>>.component1(): A =
        this[schema.First]

/** Returns second component of [this] [Tuple3]. */
@JvmName("secondOf3")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>> Struct<Tuple3<A, DA, B, DB, C, DC>>.component2(): B =
        this[schema.Second]

/** Returns third component of [this] [Tuple3]. */
@JvmName("thirdOf3")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>> Struct<Tuple3<A, DA, B, DB, C, DC>>.component3(): C =
        this[schema.Third]


// 4

/** A schema consisting of four fields. */
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

/** A shortcut for [Tuple4] with default field names. */
@JvmSynthetic // useless for Java
operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>>
        Tuple3<A, DA, B, DB, C, DC>.times(fourth: DD): Tuple4<A, DA, B, DB, C, DC, D, DD> =
        Tuple4("first", First.type, "second", Second.type, "third", Third.type, "fourth", fourth)

/** Creates an instance of a [Tuple4] according to [this] schema. */
@JvmSynthetic // returns Object[], useless for Java
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

@JvmName("newTuple")
fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>>
    newTupleBoxed(
    type: Tuple4<A, DA, B, DB, C, DC, D, DD>,
    first: A, second: B, third: C, fourth: D
): Struct<Tuple4<A, DA, B, DB, C, DC, D, DD>> =
    type(first, second, third, fourth)

/** Creates a partial instance of a [Tuple4] according to [this] schema. */
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

/** Returns first component of [this] [Tuple4]. */
@JvmName("firstOf4")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>> Struct<Tuple4<A, DA, B, DB, C, DC, D, DD>>.component1(): A =
        this[schema.First]

/** Returns second component of [this] [Tuple4]. */
@JvmName("secondOf4")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>> Struct<Tuple4<A, DA, B, DB, C, DC, D, DD>>.component2(): B =
        this[schema.Second]

/** Returns third component of [this] [Tuple4]. */
@JvmName("thirdOf4")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>> Struct<Tuple4<A, DA, B, DB, C, DC, D, DD>>.component3(): C =
        this[schema.Third]

/** Returns fourth component of [this] [Tuple4]. */
@JvmName("fourthOf4")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>> Struct<Tuple4<A, DA, B, DB, C, DC, D, DD>>.component4(): D =
        this[schema.Fourth]


// 5

/** A schema consisting of five fields. */
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

/** A shortcut for [Tuple5] with default field names. */
@JvmSynthetic // useless for Java
operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>>
        Tuple4<A, DA, B, DB, C, DC, D, DD>.times(fifth: DE): Tuple5<A, DA, B, DB, C, DC, D, DD, E, DE> =
        Tuple5("first", First.type, "second", Second.type, "third", Third.type, "fourth", Fourth.type, "fifth", fifth)

/** Creates an instance of a [Tuple5] according to [this] schema. */
@JvmSynthetic // returns Object[], useless for Java
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

@JvmName("newTuple")
fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>>
    newTupleBoxed(
    type: Tuple5<A, DA, B, DB, C, DC, D, DD, E, DE>,
    first: A, second: B, third: C, fourth: D, fifth: E
): Struct<Tuple5<A, DA, B, DB, C, DC, D, DD, E, DE>> =
    type(first, second, third, fourth, fifth)

/** Creates a partial instance of a [Tuple5] according to [this] schema. */
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

/** Returns first component of [this] [Tuple5]. */
@JvmName("firstOf5")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>> Struct<Tuple5<A, DA, B, DB, C, DC, D, DD, E, DE>>.component1(): A =
        this[schema.First]

/** Returns second component of [this] [Tuple5]. */
@JvmName("secondOf5")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>> Struct<Tuple5<A, DA, B, DB, C, DC, D, DD, E, DE>>.component2(): B =
        this[schema.Second]

/** Returns third component of [this] [Tuple5]. */
@JvmName("thirdOf5")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>> Struct<Tuple5<A, DA, B, DB, C, DC, D, DD, E, DE>>.component3(): C =
        this[schema.Third]

/** Returns fourth component of [this] [Tuple5]. */
@JvmName("fourthOf5")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>> Struct<Tuple5<A, DA, B, DB, C, DC, D, DD, E, DE>>.component4(): D =
        this[schema.Fourth]

/** Returns fifth component of [this] [Tuple5]. */
@JvmName("fifthOf5")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>> Struct<Tuple5<A, DA, B, DB, C, DC, D, DD, E, DE>>.component5(): E =
        this[schema.Fifth]


// 6

/** A schema consisting of six fields. */
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

/** A shortcut for [Tuple6] with default field names. */
@JvmSynthetic // useless for Java
operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>>
        Tuple5<A, DA, B, DB, C, DC, D, DD, E, DE>.times(sixth: DF): Tuple6<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF> =
        Tuple6("first", First.type, "second", Second.type, "third", Third.type, "fourth", Fourth.type, "fifth", Fifth.type, "sixth", sixth)

/** Creates an instance of a [Tuple6] according to [this] schema. */
@JvmSynthetic // returns Object[], useless for Java
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

@JvmName("newTuple")
fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>>
    newTupleBoxed(
    type: Tuple6<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF>,
    first: A, second: B, third: C, fourth: D, fifth: E, sixth: F
): Struct<Tuple6<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF>> =
    type(first, second, third, fourth, fifth, sixth)

/** Creates a partial instance of a [Tuple6] according to [this] schema. */
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

/** Returns first component of [this] [Tuple6]. */
@JvmName("firstOf6")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>> Struct<Tuple6<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF>>.component1(): A =
        this[schema.First]

/** Returns second component of [this] [Tuple6]. */
@JvmName("secondOf6")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>> Struct<Tuple6<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF>>.component2(): B =
        this[schema.Second]

/** Returns third component of [this] [Tuple6]. */
@JvmName("thirdOf6")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>> Struct<Tuple6<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF>>.component3(): C =
        this[schema.Third]

/** Returns fourth component of [this] [Tuple6]. */
@JvmName("fourthOf6")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>> Struct<Tuple6<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF>>.component4(): D =
        this[schema.Fourth]

/** Returns fifth component of [this] [Tuple6]. */
@JvmName("fifthOf6")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>> Struct<Tuple6<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF>>.component5(): E =
        this[schema.Fifth]

/** Returns sixth component of [this] [Tuple6]. */
@JvmName("sixthOf6")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>> Struct<Tuple6<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF>>.component6(): F =
        this[schema.Sixth]


// 7

/** A schema consisting of seven fields. */
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

/** A shortcut for [Tuple7] with default field names. */
@JvmSynthetic // useless for Java
operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>, G, DG : DataType<G>>
        Tuple6<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF>.times(seventh: DG): Tuple7<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG> =
        Tuple7("first", First.type, "second", Second.type, "third", Third.type, "fourth", Fourth.type, "fifth", Fifth.type, "sixth", Sixth.type, "seventh", seventh)

/** Creates an instance of a [Tuple7] according to [this] schema. */
@JvmSynthetic // returns Object[], useless for Java
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

@JvmName("newTuple")
fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>, G, DG : DataType<G>>
    newTupleBoxed(
    type: Tuple7<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG>,
    first: A, second: B, third: C, fourth: D, fifth: E, sixth: F, seventh: G
): Struct<Tuple7<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG>> =
    type(first, second, third, fourth, fifth, sixth, seventh)

/** Creates a partial instance of a [Tuple7] according to [this] schema. */
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

/** Returns first component of [this] [Tuple7]. */
@JvmName("firstOf7")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>, G, DG : DataType<G>> Struct<Tuple7<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG>>.component1(): A =
        this[schema.First]

/** Returns second component of [this] [Tuple7]. */
@JvmName("secondOf7")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>, G, DG : DataType<G>> Struct<Tuple7<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG>>.component2(): B =
        this[schema.Second]

/** Returns third component of [this] [Tuple7]. */
@JvmName("thirdOf7")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>, G, DG : DataType<G>> Struct<Tuple7<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG>>.component3(): C =
        this[schema.Third]

/** Returns fourth component of [this] [Tuple7]. */
@JvmName("fourthOf7")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>, G, DG : DataType<G>> Struct<Tuple7<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG>>.component4(): D =
        this[schema.Fourth]

/** Returns fifth component of [this] [Tuple7]. */
@JvmName("fifthOf7")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>, G, DG : DataType<G>> Struct<Tuple7<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG>>.component5(): E =
        this[schema.Fifth]

/** Returns sixth component of [this] [Tuple7]. */
@JvmName("sixthOf7")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>, G, DG : DataType<G>> Struct<Tuple7<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG>>.component6(): F =
        this[schema.Sixth]

/** Returns seventh component of [this] [Tuple7]. */
@JvmName("seventhOf7")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>, G, DG : DataType<G>> Struct<Tuple7<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG>>.component7(): G =
        this[schema.Seventh]


// 8

/** A schema consisting of eight fields. */
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

/** A shortcut for [Tuple8] with default field names. */
@JvmSynthetic // useless for Java
operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>, G, DG : DataType<G>, H, DH : DataType<H>>
        Tuple7<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG>.times(eighth: DH): Tuple8<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG, H, DH> =
        Tuple8("first", First.type, "second", Second.type, "third", Third.type, "fourth", Fourth.type, "fifth", Fifth.type, "sixth", Sixth.type, "seventh", Seventh.type, "eighth", eighth)

/** Creates an instance of a [Tuple8] according to [this] schema. */
@JvmSynthetic // returns Object[], useless for Java
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

@JvmName("newTuple") fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>, G, DG : DataType<G>, H, DH : DataType<H>>
    newTupleBoxed(
    type: Tuple8<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG, H, DH>,
    first: A, second: B, third: C, fourth: D, fifth: E, sixth: F, seventh: G, eighth: H
): Struct<Tuple8<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG, H, DH>> =
    type(first, second, third, fourth, fifth, sixth, seventh, eighth)

/** Creates a partial instance of a [Tuple8] according to [this] schema. */
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

/** Returns first component of [this] [Tuple8]. */
@JvmName("firstOf8")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>, G, DG : DataType<G>, H, DH : DataType<H>> Struct<Tuple8<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG, H, DH>>.component1(): A =
        this[schema.First]

/** Returns second component of [this] [Tuple8]. */
@JvmName("secondOf8")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>, G, DG : DataType<G>, H, DH : DataType<H>> Struct<Tuple8<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG, H, DH>>.component2(): B =
        this[schema.Second]

/** Returns third component of [this] [Tuple8]. */
@JvmName("thirdOf8")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>, G, DG : DataType<G>, H, DH : DataType<H>> Struct<Tuple8<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG, H, DH>>.component3(): C =
        this[schema.Third]

/** Returns fourth component of [this] [Tuple8]. */
@JvmName("fourthOf8")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>, G, DG : DataType<G>, H, DH : DataType<H>> Struct<Tuple8<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG, H, DH>>.component4(): D =
        this[schema.Fourth]

/** Returns fifth component of [this] [Tuple8]. */
@JvmName("fifthOf8")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>, G, DG : DataType<G>, H, DH : DataType<H>> Struct<Tuple8<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG, H, DH>>.component5(): E =
        this[schema.Fifth]

/** Returns sixth component of [this] [Tuple8]. */
@JvmName("sixthOf8")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>, G, DG : DataType<G>, H, DH : DataType<H>> Struct<Tuple8<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG, H, DH>>.component6(): F =
        this[schema.Sixth]

/** Returns seventh component of [this] [Tuple8]. */
@JvmName("seventhOf8")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>, G, DG : DataType<G>, H, DH : DataType<H>> Struct<Tuple8<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG, H, DH>>.component7(): G =
        this[schema.Seventh]

/** Returns eighth component of [this] [Tuple8]. */
@JvmName("eighthOf8")
inline operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>, G, DG : DataType<G>, H, DH : DataType<H>> Struct<Tuple8<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG, H, DH>>.component8(): H =
        this[schema.Eighth]


// Something red

/**
 * A placeholder to avoid undesired invocation of other overloads.
 * Despite this declaration is deprecated, it is not going to be removed.
 */
@JvmSynthetic // useless for Java
@Deprecated("Tuple9+ are not implemented", level = DeprecationLevel.ERROR)
operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>, G, DG : DataType<G>, H, DH : DataType<H>, I, DI : DataType<I>>
        Tuple8<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG, H, DH>.times(ninth: DH): Nothing =
        throw UnsupportedOperationException()

/**
 * Shows that Tuple* cannot be safely replaced with an arbitrary Schema.
 * Despite this declaration is deprecated, it is not going to be removed.
 */
@JvmSynthetic // useless for Java
@Deprecated("left operand is expected to be a Tuple*, got other Schema", level = DeprecationLevel.ERROR)
operator fun <SCH : Schema<SCH>>
        SCH.times(second: DataType<*>): Nothing =
        throw UnsupportedOperationException()

/**
 * Maintains multiplication associativity.
 * Despite this declaration is deprecated, it is not going to be removed.
 */
@JvmSynthetic // useless for Java
@Deprecated("right operand is not expected to be a Tuple* or any other Schema", level = DeprecationLevel.ERROR)
operator fun <SCH : Schema<SCH>>
        DataType<*>.times(second: SCH): Nothing =
        throw UnsupportedOperationException()
