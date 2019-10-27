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


private abstract class EitherType<T : BaseEither, SCH : Schema<SCH>>(
        schema: SCH
) : DataType.Partial<T, SCH>(schema) {

    // 'load' will create new instance of the specified Either and cannot be implemented generically

    final override fun fields(value: T): FieldSet<SCH, FieldDef<SCH, *, *>> =
            schema.fields[value._which].asFieldSet()

    final override fun store(value: T): Any? =
            value._value

}


// 2

fun <A, DA : DataType<A>, B, DB : DataType<B>> either(
        firstName: String, firstType: DA,
        secondName: String, secondType: DB
): DataType.Partial<Either<A, B>, Tuple<A, DA, B, DB>> =
        object : EitherType<Either<A, B>, Tuple<A, DA, B, DB>>(Tuple(firstName, firstType, secondName, secondType)) {

            override fun load(
                    fields: FieldSet<Tuple<A, DA, B, DB>, FieldDef<Tuple<A, DA, B, DB>, *, *>>, values: Any?
            ): Either<A, B> =
                    when (schema.single(fields).ordinal.toInt()) {
                        0 -> Either.First(values as A)
                        1 -> Either.Second(values as B)
                        else -> throw AssertionError()
                    }

        }


// 3

fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>> either3(
        firstName: String, firstType: DA,
        secondName: String, secondType: DB,
        thirdName: String, thirdType: DC
): DataType.Partial<Either3<A, B, C>, Tuple3<A, DA, B, DB, C, DC>> =
        object : EitherType<Either3<A, B, C>, Tuple3<A, DA, B, DB, C, DC>>(Tuple3(
                firstName, firstType, secondName, secondType, thirdName, thirdType
        )) {

            override fun load(
                    fields: FieldSet<Tuple3<A, DA, B, DB, C, DC>, FieldDef<Tuple3<A, DA, B, DB, C, DC>, *, *>>, values: Any?
            ): Either3<A, B, C> =
                    when (schema.single(fields).ordinal.toInt()) {
                        0 -> Either3.First(values as A)
                        1 -> Either3.Second(values as B)
                        2 -> Either3.Third(values as C)
                        else -> throw AssertionError()
                    }

        }


// 4

fun <A : Any, DA : DataType<A>, B : Any, DB : DataType<B>, C : Any, DC : DataType<C>, D, DD : DataType<D>> either4(
        firstName: String, firstType: DA,
        secondName: String, secondType: DB,
        thirdName: String, thirdType: DC,
        fourthName: String, fourthType: DD
): DataType.Partial<Either4<A, B, C, D>, Tuple4<A, DA, B, DB, C, DC, D, DD>> =
        object : EitherType<Either4<A, B, C, D>, Tuple4<A, DA, B, DB, C, DC, D, DD>>(Tuple4(
                firstName, firstType, secondName, secondType, thirdName, thirdType, fourthName, fourthType
        )) {

            override fun load(
                    fields: FieldSet<Tuple4<A, DA, B, DB, C, DC, D, DD>, FieldDef<Tuple4<A, DA, B, DB, C, DC, D, DD>, *, *>>, values: Any?
            ): Either4<A, B, C, D> =
                    when (schema.single(fields).ordinal.toInt()) {
                        0 -> Either4.First(values as A)
                        1 -> Either4.Second(values as B)
                        2 -> Either4.Third(values as C)
                        3 -> Either4.Fourth(values as D)
                        else -> throw AssertionError()
                    }

        }


// 5

fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>> either5(
        firstName: String, firstType: DA,
        secondName: String, secondType: DB,
        thirdName: String, thirdType: DC,
        fourthName: String, fourthType: DD,
        fifthName: String, fifthType: DE
): DataType.Partial<Either5<A, B, C, D, E>, Tuple5<A, DA, B, DB, C, DC, D, DD, E, DE>> =
        object : EitherType<Either5<A, B, C, D, E>, Tuple5<A, DA, B, DB, C, DC, D, DD, E, DE>>(Tuple5(
                firstName, firstType, secondName, secondType, thirdName, thirdType, fourthName, fourthType,
                fifthName, fifthType
        )) {

            override fun load(
                    fields: FieldSet<Tuple5<A, DA, B, DB, C, DC, D, DD, E, DE>, FieldDef<Tuple5<A, DA, B, DB, C, DC, D, DD, E, DE>, *, *>>, values: Any?
            ): Either5<A, B, C, D, E> =
                    when (schema.single(fields).ordinal.toInt()) {
                        0 -> Either5.First(values as A)
                        1 -> Either5.Second(values as B)
                        2 -> Either5.Third(values as C)
                        3 -> Either5.Fourth(values as D)
                        4 -> Either5.Fifth(values as E)
                        else -> throw AssertionError()
                    }

        }


// 6

fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>> either6(
        firstName: String, firstType: DA,
        secondName: String, secondType: DB,
        thirdName: String, thirdType: DC,
        fourthName: String, fourthType: DD,
        fifthName: String, fifthType: DE,
        sixthName: String, sixthType: DF
): DataType.Partial<Either6<A, B, C, D, E, F>, Tuple6<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF>> =
        object : EitherType<Either6<A, B, C, D, E, F>, Tuple6<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF>>(Tuple6(
                firstName, firstType, secondName, secondType, thirdName, thirdType, fourthName, fourthType,
                fifthName, fifthType, sixthName, sixthType
        )) {

            override fun load(
                    fields: FieldSet<Tuple6<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF>, FieldDef<Tuple6<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF>, *, *>>, values: Any?
            ): Either6<A, B, C, D, E, F> =
                    when (schema.single(fields).ordinal.toInt()) {
                        0 -> Either6.First(values as A)
                        1 -> Either6.Second(values as B)
                        2 -> Either6.Third(values as C)
                        3 -> Either6.Fourth(values as D)
                        4 -> Either6.Fifth(values as E)
                        5 -> Either6.Sixth(values as F)
                        else -> throw AssertionError()
                    }

        }


// 7

fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>, G, DG : DataType<G>> either7(
        firstName: String, firstType: DA,
        secondName: String, secondType: DB,
        thirdName: String, thirdType: DC,
        fourthName: String, fourthType: DD,
        fifthName: String, fifthType: DE,
        sixthName: String, sixthType: DF,
        seventhName: String, seventhType: DG
): DataType.Partial<Either7<A, B, C, D, E, F, G>, Tuple7<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG>> =
        object : EitherType<Either7<A, B, C, D, E, F, G>, Tuple7<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG>>(Tuple7(
                firstName, firstType, secondName, secondType, thirdName, thirdType, fourthName, fourthType,
                fifthName, fifthType, sixthName, sixthType, seventhName, seventhType
        )) {

            override fun load(
                    fields: FieldSet<Tuple7<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG>, FieldDef<Tuple7<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG>, *, *>>, values: Any?
            ): Either7<A, B, C, D, E, F, G> =
                    when (schema.single(fields).ordinal.toInt()) {
                        0 -> Either7.First(values as A)
                        1 -> Either7.Second(values as B)
                        2 -> Either7.Third(values as C)
                        3 -> Either7.Fourth(values as D)
                        4 -> Either7.Fifth(values as E)
                        5 -> Either7.Sixth(values as F)
                        6 -> Either7.Seventh(values as G)
                        else -> throw AssertionError()
                    }

        }


// 8

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
        object : EitherType<Either8<A, B, C, D, E, F, G, H>, Tuple8<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG, H, DH>>(Tuple8(
                firstName, firstType, secondName, secondType, thirdName, thirdType, fourthName, fourthType,
                fifthName, fifthType, sixthName, sixthType, seventhName, seventhType, eighthName, eighthType
        )) {

            override fun load(
                    fields: FieldSet<Tuple8<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG, H, DH>, FieldDef<Tuple8<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG, H, DH>, *, *>>, values: Any?
            ): Either8<A, B, C, D, E, F, G, H> =
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

        }
