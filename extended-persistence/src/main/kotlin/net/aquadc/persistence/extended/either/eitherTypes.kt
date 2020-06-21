@file:JvmName("EitherTypes")
@file:Suppress("UNCHECKED_CAST") // ugh, just many of them
package net.aquadc.persistence.extended.either

import net.aquadc.persistence.extended.Tuple
import net.aquadc.persistence.extended.Tuple3
import net.aquadc.persistence.extended.Tuple4
import net.aquadc.persistence.extended.Tuple5
import net.aquadc.persistence.extended.Tuple6
import net.aquadc.persistence.extended.Tuple7
import net.aquadc.persistence.extended.Tuple8
import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.FieldSet
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.ordinal
import net.aquadc.persistence.struct.single
import net.aquadc.persistence.type.DataType


private class EitherType<A, B, C, D, E, F, G, H, SCH : Schema<SCH>>(
        schema: SCH
) : DataType.NotNull.Partial<Either8<A, B, C, D, E, F, G, H>, SCH>(schema) {

    override fun load(fields: FieldSet<SCH, *>, values: Any?): Either8<A, B, C, D, E, F, G, H> =
            when (schema.single(fields as FieldSet<SCH, FieldDef<SCH, *, *>>).ordinal) {
                0 -> Either8.First(values as A)
                1 -> Either8.Second(values as B)
                2 -> Either8.Third(values as C)
                3 -> Either8.Fourth(values as D)
                4 -> Either8.Fifth(values as E)
                5 -> Either8.Sixth(values as F)
                6 -> Either8.Seventh(values as G)
                7 -> Either8.Eighth(values as H)
                else -> throw AssertionError()
            }

    override fun fields(value: Either8<A, B, C, D, E, F, G, H>): FieldSet<SCH, FieldDef<SCH, *, *>> =
        TODO("Back-end (JVM) Internal error") // schema.fieldAt(value._which).asFieldSet()

    override fun store(value: Either8<A, B, C, D, E, F, G, H>): Any? =
            value._value

}


fun <A, DA : DataType<A>, B, DB : DataType<B>> either(
        firstName: String, firstType: DA,
        secondName: String, secondType: DB
): DataType.NotNull.Partial<Either<A, B>, Tuple<A, DA, B, DB>> =
        EitherType(Tuple(firstName, firstType, secondName, secondType))

@JvmSynthetic // useless for Java
operator fun <A, DA : DataType<A>, B, DB : DataType<B>>
        DA.plus(second: DB): DataType.NotNull.Partial<Either<A, B>, Tuple<A, DA, B, DB>> =
        either("first", this, "second", second)


fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>> either3(
        firstName: String, firstType: DA,
        secondName: String, secondType: DB,
        thirdName: String, thirdType: DC
): DataType.NotNull.Partial<Either3<A, B, C>, Tuple3<A, DA, B, DB, C, DC>> =
        EitherType(Tuple3(
                firstName, firstType, secondName, secondType, thirdName, thirdType
        ))

@JvmSynthetic // useless for Java
operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>>
    DataType.NotNull.Partial<Either<A, B>, Tuple<A, DA, B, DB>>.plus(third: DC): DataType.NotNull.Partial<Either3<A, B, C>, Tuple3<A, DA, B, DB, C, DC>> =
        either3("first", schema.run { First.type }, "second", schema.run { Second.type }, "third", third)


fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>> either4(
        firstName: String, firstType: DA,
        secondName: String, secondType: DB,
        thirdName: String, thirdType: DC,
        fourthName: String, fourthType: DD
): DataType.NotNull.Partial<Either4<A, B, C, D>, Tuple4<A, DA, B, DB, C, DC, D, DD>> =
        EitherType(Tuple4(
                firstName, firstType, secondName, secondType, thirdName, thirdType, fourthName, fourthType
        ))

@JvmSynthetic // useless for Java
@JvmName("e3plus") operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>>
    DataType.NotNull.Partial<Either3<A, B, C>, Tuple3<A, DA, B, DB, C, DC>>.plus(fourth: DD): DataType.NotNull.Partial<Either4<A, B, C, D>, Tuple4<A, DA, B, DB, C, DC, D, DD>> =
        either4("first", schema.run { First.type }, "second", schema.run { Second.type }, "third", schema.run { Third.type }, "fourth", fourth)


fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>> either5(
        firstName: String, firstType: DA,
        secondName: String, secondType: DB,
        thirdName: String, thirdType: DC,
        fourthName: String, fourthType: DD,
        fifthName: String, fifthType: DE
): DataType.NotNull.Partial<Either5<A, B, C, D, E>, Tuple5<A, DA, B, DB, C, DC, D, DD, E, DE>> =
        EitherType(Tuple5(
                firstName, firstType, secondName, secondType, thirdName, thirdType, fourthName, fourthType,
                fifthName, fifthType
        ))

@JvmSynthetic // useless for Java
@JvmName("e4plus") operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>>
    DataType.NotNull.Partial<Either4<A, B, C, D>, Tuple4<A, DA, B, DB, C, DC, D, DD>>.plus(fifth: DE): DataType.NotNull.Partial<Either5<A, B, C, D, E>, Tuple5<A, DA, B, DB, C, DC, D, DD, E, DE>> =
        either5("first", schema.run { First.type }, "second", schema.run { Second.type }, "third", schema.run { Third.type }, "fourth", schema.run { Fourth.type }, "fifth", fifth)


fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>> either6(
        firstName: String, firstType: DA,
        secondName: String, secondType: DB,
        thirdName: String, thirdType: DC,
        fourthName: String, fourthType: DD,
        fifthName: String, fifthType: DE,
        sixthName: String, sixthType: DF
): DataType.NotNull.Partial<Either6<A, B, C, D, E, F>, Tuple6<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF>> =
        EitherType(Tuple6(
                firstName, firstType, secondName, secondType, thirdName, thirdType, fourthName, fourthType,
                fifthName, fifthType, sixthName, sixthType
        ))

@JvmSynthetic // useless for Java
@JvmName("e5plus") operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>>
    DataType.NotNull.Partial<Either5<A, B, C, D, E>, Tuple5<A, DA, B, DB, C, DC, D, DD, E, DE>>.plus(sixth: DF): DataType.NotNull.Partial<Either6<A, B, C, D, E, F>, Tuple6<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF>> =
        either6("first", schema.run { First.type }, "second", schema.run { Second.type }, "third", schema.run { Third.type }, "fourth", schema.run { Fourth.type }, "fifth", schema.run { Fifth.type }, "sixth", sixth)


fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>, G, DG : DataType<G>> either7(
        firstName: String, firstType: DA,
        secondName: String, secondType: DB,
        thirdName: String, thirdType: DC,
        fourthName: String, fourthType: DD,
        fifthName: String, fifthType: DE,
        sixthName: String, sixthType: DF,
        seventhName: String, seventhType: DG
): DataType.NotNull.Partial<Either7<A, B, C, D, E, F, G>, Tuple7<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG>> =
        EitherType(Tuple7(
                firstName, firstType, secondName, secondType, thirdName, thirdType, fourthName, fourthType,
                fifthName, fifthType, sixthName, sixthType, seventhName, seventhType
        ))

@JvmSynthetic // useless for Java
@JvmName("e6plus") operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>, G, DG : DataType<G>>
    DataType.NotNull.Partial<Either6<A, B, C, D, E, F>, Tuple6<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF>>.plus(seventh: DG): DataType.NotNull.Partial<Either7<A, B, C, D, E, F, G>, Tuple7<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG>> =
        either7("first", schema.run { First.type }, "second", schema.run { Second.type }, "third", schema.run { Third.type }, "fourth", schema.run { Fourth.type }, "fifth", schema.run { Fifth.type }, "sixth", schema.run { Sixth.type }, "seventh", seventh)


fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>, G, DG : DataType<G>, H, DH : DataType<H>> either8(
        firstName: String, firstType: DA,
        secondName: String, secondType: DB,
        thirdName: String, thirdType: DC,
        fourthName: String, fourthType: DD,
        fifthName: String, fifthType: DE,
        sixthName: String, sixthType: DF,
        seventhName: String, seventhType: DG,
        eighthName: String, eighthType: DH
): DataType.NotNull.Partial<Either8<A, B, C, D, E, F, G, H>, Tuple8<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG, H, DH>> =
        EitherType(Tuple8(
                firstName, firstType, secondName, secondType, thirdName, thirdType, fourthName, fourthType,
                fifthName, fifthType, sixthName, sixthType, seventhName, seventhType, eighthName, eighthType
        ))

@JvmSynthetic // useless for Java
@JvmName("e7plus") operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>, G, DG : DataType<G>, H, DH : DataType<H>>
    DataType.NotNull.Partial<Either7<A, B, C, D, E, F, G>, Tuple7<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG>>.plus(eighth: DH): DataType.NotNull.Partial<Either8<A, B, C, D, E, F, G, H>, Tuple8<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG, H, DH>> =
        either8("first", schema.run { First.type }, "second", schema.run { Second.type }, "third", schema.run { Third.type }, "fourth", schema.run { Fourth.type }, "fifth", schema.run { Fifth.type }, "sixth", schema.run { Sixth.type }, "seventh", schema.run { Seventh.type }, "eighth", eighth)


/**
 * A placeholder to surprise less.
 * Despite this declaration is deprecated, it is not going to be removed.
 */
@JvmSynthetic // useless for Java
@JvmName("e8plus") @Deprecated("Either9+ are not implemented", level = DeprecationLevel.ERROR) operator fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>, G, DG : DataType<G>, H, DH : DataType<H>, I, DI : DataType<I>>
    DataType.NotNull.Partial<Either8<A, B, C, D, E, F, G, H>, Tuple8<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG, H, DH>>.plus(ninth: DI): Nothing =
        throw UnsupportedOperationException()

/**
 * Maintains addition associativity.
 * Despite this declaration is deprecated, it is not going to be removed.
 */
@JvmSynthetic // useless for Java
@JvmName("plusE") @Deprecated("right operand is not expected to be Either", level = DeprecationLevel.ERROR) operator fun
    DataType.NotNull.Partial<out Either8<*, *, *, *, *, *, *, *>, out Tuple8<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>>
        .plus(other: DataType.NotNull.Partial<out Either8<*, *, *, *, *, *, *, *>, out Tuple8<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>>): Nothing =
        throw UnsupportedOperationException()
