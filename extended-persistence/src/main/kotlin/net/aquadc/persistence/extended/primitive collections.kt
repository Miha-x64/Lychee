@file:JvmName("PrimitiveCollections")
package net.aquadc.persistence.extended

import net.aquadc.persistence.type.AnyCollection
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.f64
import net.aquadc.persistence.type.f32
import net.aquadc.persistence.type.i32
import net.aquadc.persistence.type.i64


/**
 * Stores [IntArray] instances as collections of [Int]s.
 */
@JvmField
val intCollection: DataType.NotNull.Collect<IntArray, Int, DataType.NotNull.Simple<Int>> = ArrayNoOp(i32)

/**
 * Stores [LongArray] instances as collections of [Long]s.
 */
@JvmField
val longCollection: DataType.NotNull.Collect<LongArray, Long, DataType.NotNull.Simple<Long>> = ArrayNoOp(i64)

/**
 * Stores [FloatArray] instances as collections of [Float]s.
 */
@JvmField
val floatCollection: DataType.NotNull.Collect<FloatArray, Float, DataType.NotNull.Simple<Float>> = ArrayNoOp(f32)

/**
 * Stores [DoubleArray] instances as collections of [Double]s.
 */
@JvmField
val doubleCollection: DataType.NotNull.Collect<DoubleArray, Double, DataType.NotNull.Simple<Double>> = ArrayNoOp(f64)


private class ArrayNoOp<C, E>(type: Simple<E>) : DataType.NotNull.Collect<C, E, DataType.NotNull.Simple<E>>(type) {

    override fun load(value: AnyCollection): C {
        val kind = (elementType as Simple).kind
        return when (value) {
            is Collection<*> -> coerceToArray(value, kind)
            is Array<*> -> coerceToArray(value, kind)
            else -> (value as C).also(::sanityCheck)
        }
    }

    override fun store(value: C): AnyCollection {
        sanityCheck(value)
        return value as AnyCollection
    }

    private fun coerceToArray(value: Collection<*>, kind: Simple.Kind): C =
            when (kind) {
                Simple.Kind.Bool -> throw AssertionError()
                Simple.Kind.I32 -> (value as List<Int>).toIntArray()
                Simple.Kind.I64 -> (value as List<Long>).toLongArray()
                Simple.Kind.F32 -> (value as List<Float>).toFloatArray()
                Simple.Kind.F64 -> (value as List<Double>).toDoubleArray()
                Simple.Kind.Str -> throw AssertionError()
                Simple.Kind.Blob -> throw AssertionError()
                else -> throw AssertionError()
            } as C

    private fun coerceToArray(value: Array<*>, kind: Simple.Kind): C =
            when (kind) {
                Simple.Kind.Bool -> throw AssertionError()
                Simple.Kind.I32 -> (value as Array<Int>).toIntArray()
                Simple.Kind.I64 -> (value as Array<Long>).toLongArray()
                Simple.Kind.F32 -> (value as Array<Float>).toFloatArray()
                Simple.Kind.F64 -> (value as Array<Double>).toDoubleArray()
                Simple.Kind.Str -> throw AssertionError()
                Simple.Kind.Blob -> throw AssertionError()
                else -> throw AssertionError()
            } as C

    private fun sanityCheck(value: Any?) {
        when ((elementType as Simple).kind) {
            Simple.Kind.Bool -> throw AssertionError()
            Simple.Kind.I32 -> value as IntArray
            Simple.Kind.I64 -> value as LongArray
            Simple.Kind.F32 -> value as FloatArray
            Simple.Kind.F64 -> value as DoubleArray
            Simple.Kind.Str -> throw AssertionError()
            Simple.Kind.Blob -> throw AssertionError()
            else -> throw AssertionError()
        }
    }

}
