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

fun <A, B> either(
        firstName: String, firstType: DataType<A>,
        secondName: String, secondType: DataType<B>
): DataType.Partial<Either<A, B>, Tuple<A, B>> =
        object : EitherType<Either<A, B>, Tuple<A, B>>(Tuple(firstName, firstType, secondName, secondType)) {

            override fun load(
                    fields: FieldSet<Tuple<A, B>, FieldDef<Tuple<A, B>, *, *>>, values: Any?
            ): Either<A, B> =
                    when (schema.single(fields).ordinal.toInt()) {
                        0 -> Either.First(values as A)
                        1 -> Either.Second(values as B)
                        else -> throw AssertionError()
                    }

        }


// 3

fun <A, B, C> either3(
        firstName: String, firstType: DataType<A>,
        secondName: String, secondType: DataType<B>,
        thirdName: String, thirdType: DataType<C>
): DataType.Partial<Either3<A, B, C>, Tuple3<A, B, C>> =
        object : EitherType<Either3<A, B, C>, Tuple3<A, B, C>>(Tuple3(
                firstName, firstType, secondName, secondType, thirdName, thirdType
        )) {

            override fun load(
                    fields: FieldSet<Tuple3<A, B, C>, FieldDef<Tuple3<A, B, C>, *, *>>, values: Any?
            ): Either3<A, B, C> =
                    when (schema.single(fields).ordinal.toInt()) {
                        0 -> Either3.First(values as A)
                        1 -> Either3.Second(values as B)
                        2 -> Either3.Third(values as C)
                        else -> throw AssertionError()
                    }

        }


// 4

fun <A, B, C, D> either4(
        firstName: String, firstType: DataType<A>,
        secondName: String, secondType: DataType<B>,
        thirdName: String, thirdType: DataType<C>,
        fourthName: String, fourthType: DataType<D>
): DataType.Partial<Either4<A, B, C, D>, Tuple4<A, B, C, D>> =
        object : EitherType<Either4<A, B, C, D>, Tuple4<A, B, C, D>>(Tuple4(
                firstName, firstType, secondName, secondType, thirdName, thirdType, fourthName, fourthType
        )) {

            override fun load(
                    fields: FieldSet<Tuple4<A, B, C, D>, FieldDef<Tuple4<A, B, C, D>, *, *>>, values: Any?
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

fun <A, B, C, D, E> either5(
        firstName: String, firstType: DataType<A>,
        secondName: String, secondType: DataType<B>,
        thirdName: String, thirdType: DataType<C>,
        fourthName: String, fourthType: DataType<D>,
        fifthName: String, fifthType: DataType<E>
): DataType.Partial<Either5<A, B, C, D, E>, Tuple5<A, B, C, D, E>> =
        object : EitherType<Either5<A, B, C, D, E>, Tuple5<A, B, C, D, E>>(Tuple5(
                firstName, firstType, secondName, secondType, thirdName, thirdType, fourthName, fourthType,
                fifthName, fifthType
        )) {

            override fun load(
                    fields: FieldSet<Tuple5<A, B, C, D, E>, FieldDef<Tuple5<A, B, C, D, E>, *, *>>, values: Any?
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

fun <A, B, C, D, E, F> either6(
        firstName: String, firstType: DataType<A>,
        secondName: String, secondType: DataType<B>,
        thirdName: String, thirdType: DataType<C>,
        fourthName: String, fourthType: DataType<D>,
        fifthName: String, fifthType: DataType<E>,
        sixthName: String, sixthType: DataType<F>
): DataType.Partial<Either6<A, B, C, D, E, F>, Tuple6<A, B, C, D, E, F>> =
        object : EitherType<Either6<A, B, C, D, E, F>, Tuple6<A, B, C, D, E, F>>(Tuple6(
                firstName, firstType, secondName, secondType, thirdName, thirdType, fourthName, fourthType,
                fifthName, fifthType, sixthName, sixthType
        )) {

            override fun load(
                    fields: FieldSet<Tuple6<A, B, C, D, E, F>, FieldDef<Tuple6<A, B, C, D, E, F>, *, *>>, values: Any?
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

fun <A, B, C, D, E, F, G> either7(
        firstName: String, firstType: DataType<A>,
        secondName: String, secondType: DataType<B>,
        thirdName: String, thirdType: DataType<C>,
        fourthName: String, fourthType: DataType<D>,
        fifthName: String, fifthType: DataType<E>,
        sixthName: String, sixthType: DataType<F>,
        seventhName: String, seventhType: DataType<G>
): DataType.Partial<Either7<A, B, C, D, E, F, G>, Tuple7<A, B, C, D, E, F, G>> =
        object : EitherType<Either7<A, B, C, D, E, F, G>, Tuple7<A, B, C, D, E, F, G>>(Tuple7(
                firstName, firstType, secondName, secondType, thirdName, thirdType, fourthName, fourthType,
                fifthName, fifthType, sixthName, sixthType, seventhName, seventhType
        )) {

            override fun load(
                    fields: FieldSet<Tuple7<A, B, C, D, E, F, G>, FieldDef<Tuple7<A, B, C, D, E, F, G>, *, *>>, values: Any?
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

fun <A, B, C, D, E, F, G, H> either8(
        firstName: String, firstType: DataType<A>,
        secondName: String, secondType: DataType<B>,
        thirdName: String, thirdType: DataType<C>,
        fourthName: String, fourthType: DataType<D>,
        fifthName: String, fifthType: DataType<E>,
        sixthName: String, sixthType: DataType<F>,
        seventhName: String, seventhType: DataType<G>,
        eighthName: String, eighthType: DataType<H>
): DataType.Partial<Either8<A, B, C, D, E, F, G, H>, Tuple8<A, B, C, D, E, F, G, H>> =
        object : EitherType<Either8<A, B, C, D, E, F, G, H>, Tuple8<A, B, C, D, E, F, G, H>>(Tuple8(
                firstName, firstType, secondName, secondType, thirdName, thirdType, fourthName, fourthType,
                fifthName, fifthType, sixthName, sixthType, seventhName, seventhType, eighthName, eighthType
        )) {

            override fun load(
                    fields: FieldSet<Tuple8<A, B, C, D, E, F, G, H>, FieldDef<Tuple8<A, B, C, D, E, F, G, H>, *, *>>, values: Any?
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
