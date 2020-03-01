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
import net.aquadc.persistence.struct.asFieldSet
import net.aquadc.persistence.struct.single
import net.aquadc.persistence.type.DataType


private class EitherType<A, B, C, D, E, F, G, H, SCH : Schema<SCH>>(
        schema: SCH
) : DataType.Partial<Either8<A, B, C, D, E, F, G, H>, SCH>(schema) {

    override fun load(fields: FieldSet<SCH, FieldDef<SCH, *, *>>, values: Any?): Either8<A, B, C, D, E, F, G, H> =
            when (schema.single(fields).ordinal.toInt()) {
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
            schema.fields[value._which].asFieldSet()

    override fun store(value: Either8<A, B, C, D, E, F, G, H>): Any? =
            value._value

}


fun <A, DA : DataType<A>, B, DB : DataType<B>> either(
        firstName: String, firstType: DA,
        secondName: String, secondType: DB
): DataType.Partial<Either<A, B>, Tuple<A, DA, B, DB>> =
        EitherType(Tuple(firstName, firstType, secondName, secondType))

fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>> either3(
        firstName: String, firstType: DA,
        secondName: String, secondType: DB,
        thirdName: String, thirdType: DC
): DataType.Partial<Either3<A, B, C>, Tuple3<A, DA, B, DB, C, DC>> =
        EitherType(Tuple3(
                firstName, firstType, secondName, secondType, thirdName, thirdType
        ))

fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>> either4(
        firstName: String, firstType: DA,
        secondName: String, secondType: DB,
        thirdName: String, thirdType: DC,
        fourthName: String, fourthType: DD
): DataType.Partial<Either4<A, B, C, D>, Tuple4<A, DA, B, DB, C, DC, D, DD>> =
        EitherType(Tuple4(
                firstName, firstType, secondName, secondType, thirdName, thirdType, fourthName, fourthType
        ))

fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>> either5(
        firstName: String, firstType: DA,
        secondName: String, secondType: DB,
        thirdName: String, thirdType: DC,
        fourthName: String, fourthType: DD,
        fifthName: String, fifthType: DE
): DataType.Partial<Either5<A, B, C, D, E>, Tuple5<A, DA, B, DB, C, DC, D, DD, E, DE>> =
        EitherType(Tuple5(
                firstName, firstType, secondName, secondType, thirdName, thirdType, fourthName, fourthType,
                fifthName, fifthType
        ))

fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>> either6(
        firstName: String, firstType: DA,
        secondName: String, secondType: DB,
        thirdName: String, thirdType: DC,
        fourthName: String, fourthType: DD,
        fifthName: String, fifthType: DE,
        sixthName: String, sixthType: DF
): DataType.Partial<Either6<A, B, C, D, E, F>, Tuple6<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF>> =
        EitherType(Tuple6(
                firstName, firstType, secondName, secondType, thirdName, thirdType, fourthName, fourthType,
                fifthName, fifthType, sixthName, sixthType
        ))

fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>, G, DG : DataType<G>> either7(
        firstName: String, firstType: DA,
        secondName: String, secondType: DB,
        thirdName: String, thirdType: DC,
        fourthName: String, fourthType: DD,
        fifthName: String, fifthType: DE,
        sixthName: String, sixthType: DF,
        seventhName: String, seventhType: DG
): DataType.Partial<Either7<A, B, C, D, E, F, G>, Tuple7<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG>> =
        EitherType(Tuple7(
                firstName, firstType, secondName, secondType, thirdName, thirdType, fourthName, fourthType,
                fifthName, fifthType, sixthName, sixthType, seventhName, seventhType
        ))

fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>, G, DG : DataType<G>, H, DH : DataType<H>> either8(
        firstName: String, firstType: DA,
        secondName: String, secondType: DB,
        thirdName: String, thirdType: DC,
        fourthName: String, fourthType: DD,
        fifthName: String, fifthType: DE,
        sixthName: String, sixthType: DF,
        seventhName: String, seventhType: DG,
        eighthName: String, eighthType: DH
): DataType.Partial<Either8<A, B, C, D, E, F, G, H>, Tuple8<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG, H, DH>> =
        EitherType(Tuple8(
                firstName, firstType, secondName, secondType, thirdName, thirdType, fourthName, fourthType,
                fifthName, fifthType, sixthName, sixthType, seventhName, seventhType, eighthName, eighthType
        ))
