@file:JvmName("PrimitiveCollections")
package net.aquadc.persistence.extended

import net.aquadc.persistence.type.AnyCollection
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.i8
import net.aquadc.persistence.type.f64
import net.aquadc.persistence.type.f32
import net.aquadc.persistence.type.i32
import net.aquadc.persistence.type.i64
import net.aquadc.persistence.type.i16


/**
 * Stores [ByteArray] instances as collections of [Byte]s.
 */
@JvmField @Deprecated("does not look very useful")
val byteCollection: DataType.Collect<ByteArray, Byte, DataType.Simple<Byte>> = ArrayNoOp(i8)

/**
 * Stores [ShortArray] instances as collections of [Short]s.
 */
@JvmField @Deprecated("does not look very useful")
val shortCollection: DataType.Collect<ShortArray, Short, DataType.Simple<Short>> = ArrayNoOp(i16)

/**
 * Stores [IntArray] instances as collections of [Int]s.
 */
@JvmField
val intCollection: DataType.Collect<IntArray, Int, DataType.Simple<Int>> = ArrayNoOp(i32)

/**
 * Stores [LongArray] instances as collections of [Long]s.
 */
@JvmField
val longCollection: DataType.Collect<LongArray, Long, DataType.Simple<Long>> = ArrayNoOp(i64)

/**
 * Stores [FloatArray] instances as collections of [Float]s.
 */
@JvmField
val floatCollection: DataType.Collect<FloatArray, Float, DataType.Simple<Float>> = ArrayNoOp(f32)

/**
 * Stores [DoubleArray] instances as collections of [Double]s.
 */
@JvmField
val doubleCollection: DataType.Collect<DoubleArray, Double, DataType.Simple<Double>> = ArrayNoOp(f64)


private class ArrayNoOp<C, E>(type: Simple<E>) : DataType.Collect<C, E, DataType.Simple<E>>(type) {

    override fun load(value: AnyCollection): C {
        val kind = elementType.kind
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
                Simple.Kind.I8 -> (value as List<Byte>).toByteArray()
                Simple.Kind.I16 -> (value as List<Short>).toShortArray()
                Simple.Kind.I32 -> (value as List<Int>).toIntArray()
                Simple.Kind.I64 -> (value as List<Long>).toLongArray()
                Simple.Kind.F32 -> (value as List<Float>).toFloatArray()
                Simple.Kind.F64 -> (value as List<Double>).toDoubleArray()
                Simple.Kind.Str -> throw AssertionError()
                Simple.Kind.Blob -> throw AssertionError()
            } as C

    private fun coerceToArray(value: Array<*>, kind: Simple.Kind): C =
            when (kind) {
                Simple.Kind.Bool -> throw AssertionError()
                Simple.Kind.I8 -> (value as Array<Byte>).toByteArray()
                Simple.Kind.I16 -> (value as Array<Short>).toShortArray()
                Simple.Kind.I32 -> (value as Array<Int>).toIntArray()
                Simple.Kind.I64 -> (value as Array<Long>).toLongArray()
                Simple.Kind.F32 -> (value as Array<Float>).toFloatArray()
                Simple.Kind.F64 -> (value as Array<Double>).toDoubleArray()
                Simple.Kind.Str -> throw AssertionError()
                Simple.Kind.Blob -> throw AssertionError()
            } as C

    private fun sanityCheck(value: Any?) {
        when (elementType.kind) {
            Simple.Kind.Bool -> throw AssertionError()
            Simple.Kind.I8 -> value as ByteArray
            Simple.Kind.I16 -> value as ShortArray
            Simple.Kind.I32 -> value as IntArray
            Simple.Kind.I64 -> value as LongArray
            Simple.Kind.F32 -> value as FloatArray
            Simple.Kind.F64 -> value as DoubleArray
            Simple.Kind.Str -> throw AssertionError()
            Simple.Kind.Blob -> throw AssertionError()
        }
    }

}
