package net.aquadc.persistence.type


/**
 * Represents a way of storing a value.
 */
sealed class DataType<T>(
        val isNullable: Boolean
) {

    /**
     * @param sizeBits can be 1, 8, 16, 32, or 64, and doesn't depend on [isNullable], i. e. nullability info is ignored
     */
    abstract class Integer<T> internal constructor(isNullable: Boolean, val sizeBits: Int) : DataType<T>(isNullable) {

        /**
         * @return [value] as a primitive type.
         * @throws NullPointerException if [value] is null.
         */
        abstract fun asLong(value: T): Long

        /**
         * @return [value] as [T]
         */
        abstract fun asT(value: Long): T

    }

    /**
     * @param sizeBits can be 32 or 64, doesn't depend on [isNullable]
     */
    abstract class Float<T> internal constructor(isNullable: Boolean, val sizeBits: Int) : DataType<T>(isNullable) {

        /**
         * @return [value] as a [Double]
         * @throws NullPointerException if [value] is `null`
         */
        abstract fun asDouble(value: T): Double

        /**
         * @return [value] as [T]
         */
        abstract fun asT(value: Double): T

    }

    /**
     * @param maxLengthChars can be [Byte.MAX_VALUE], [Short.MAX_VALUE], or [Int.MAX_VALUE], and used in SQL
     */
    abstract class String<T> internal constructor(isNullable: Boolean, val maxLengthChars: Int) : DataType<T>(isNullable) {

        /**
         * @return [value] as a [kotlin.String]
         * @throws NullPointerException if [value] is `null`
         */
        abstract fun asString(value: T): kotlin.String

        /**
         * @return [value] as [T]
         */
        abstract fun asT(value: kotlin.String): T

    }

    /**
     * @param maxLength can be [Byte.MAX_VALUE], [Short.MAX_VALUE], or [Int.MAX_VALUE], and used in SQL
     */
    abstract class Blob<T> internal constructor(isNullable: Boolean, val maxLength: Int) : DataType<T>(isNullable) {

        /**
         * @return [value] as [ByteArray]
         * @throws NullPointerException if [value] is `null`
         */
        abstract fun asByteArray(value: T): ByteArray

        /**
         * @return [value] as [T]
         */
        abstract fun asT(value: ByteArray): T

    }

//    abstract class List<L, E> internal constructor(isNullable: Boolean, eType: DataType<E>) : DataType<L>(isNullable) TODO
//    abstract class Map<M, K, V> internal constructor(isNullable: Boolean, keyType: DataType<K>, valueType: DataType<K>) : DataType<M>(isNullable) TODO
//    abstract class Union<T> internal constructor(isNullable: Boolean, types: List<DataType<out T>>) : DataType<T>(isNullable) TODO
//    Date is not supported because it's mutable and the most parts of it are deprecated 20+ years ago. TODO consider


}

private class Ints<T>(isNullable: Boolean, sizeBits: Int) : DataType.Integer<T>(isNullable, sizeBits) {

    override fun asLong(value: T): Long {
        if (value === null) throw NullPointerException()
        return when (sizeBits) {
            1 -> if (value as Boolean) 1 else 0
            8 -> (value as Byte).toLong()
            16 -> (value as Short).toLong()
            32 -> (value as Int).toLong()
            64 -> value as Long
            else -> throw AssertionError()
        }
    }

    @Suppress("IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST")
    override fun asT(value: Long): T = when (sizeBits) {
        1 -> value == 1L
        8 -> value.toByte()
        16 -> value.toShort()
        32 -> value.toInt()
        64 -> value
        else -> throw AssertionError()
    } as T

}

@JvmField val bool: DataType.Integer<Boolean> = Ints(false, 1)
@JvmField val nullableBool: DataType.Integer<Boolean?> = Ints(true, 1)

@JvmField val byte: DataType.Integer<Byte> = Ints(false, 8)
@JvmField val nullableByte: DataType.Integer<Byte?> = Ints(true, 8)

@JvmField val short: DataType.Integer<Short> = Ints(false, 16)
@JvmField val nullableShort: DataType.Integer<Short?> = Ints(true, 16)

@JvmField val int: DataType.Integer<Int> = Ints(false, 32)
@JvmField val nullableInt: DataType.Integer<Int?> = Ints(true, 32)

@JvmField val long: DataType.Integer<Long> = Ints(false, 64)
@JvmField val nullableLong: DataType.Integer<Long?> = Ints(true, 64)


private class Floats<T>(isNullable: Boolean, sizeBits: Int) : DataType.Float<T>(isNullable, sizeBits) {

    override fun asDouble(value: T): Double {
        if (value === null) throw NullPointerException()
        return when (sizeBits) {
            32 -> (value as kotlin.Float).toDouble()
            64 -> value as Double
            else -> throw AssertionError()
        }
    }

    @Suppress("IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST")
    override fun asT(value: Double): T = when (sizeBits) {
        32 -> value.toFloat()
        64 -> value
        else -> throw AssertionError()
    } as T

}

@JvmField val float: DataType<Float> = Floats(false, 32)
@JvmField val nullableFloat: DataType<Float?> = Floats(true, 32)

@JvmField val double: DataType<Double> = Floats(false, 64)
@JvmField val nullableDouble: DataType<Double?> = Floats(true, 64)


private class Strings<T>(isNullable: Boolean, maxLengthChars: Int) : DataType.String<T>(isNullable, maxLengthChars) {

    override fun asString(value: T): kotlin.String =
            if (value === null) throw NullPointerException() else value as kotlin.String

    @Suppress("UNCHECKED_CAST")
    override fun asT(value: kotlin.String): T =
            value as T

}

/*
@JvmField val smallString: DataType<String> = Strings(false, Byte.MAX_VALUE.toInt())
@JvmField val smallNullableString: DataType<String?> = Strings(false, Byte.MAX_VALUE.toInt())

@JvmField val mediumString: DataType<String> = Strings(false, Short.MAX_VALUE.toInt())
@JvmField val mediumNullableString: DataType<String?> = Strings(false, Short.MAX_VALUE.toInt())

@JvmField val largeString: DataType<String> = Strings(false, Int.MAX_VALUE)
@JvmField val largeNullableString: DataType<String?> = Strings(false, Int.MAX_VALUE)
*/
@JvmField val string: DataType<String> = Strings(false, Int.MAX_VALUE)
@JvmField val nullableString: DataType<String?> = Strings(false, Int.MAX_VALUE)


private class Bytes<T>(isNullable: Boolean, maxLength: Int) : DataType.Blob<T>(isNullable, maxLength) {

    override fun asByteArray(value: T): ByteArray =
            if (value === null) throw NullPointerException() else value as ByteArray

    @Suppress("UNCHECKED_CAST")
    override fun asT(value: ByteArray): T =
            value as T

}

private const val bytesMessage =
        "Note: if you mutate array, we won't notice — you must set() it in a transaction. " +
                "Consider using immutable ByteString instead."

/*
@JvmField val smallByteArray: DataType<ByteArray> = Bytes(false, Byte.MAX_VALUE.toInt())
@JvmField val smallNullableByteArray: DataType<ByteArray?> = Bytes(false, Byte.MAX_VALUE.toInt())

@JvmField val mediumByteArray: DataType<ByteArray> = Bytes(false, Short.MAX_VALUE.toInt())
@JvmField val mediumNullableByteArray: DataType<ByteArray?> = Bytes(false, Short.MAX_VALUE.toInt())

@JvmField val largeByteArray: DataType<ByteArray> = Bytes(false, Int.MAX_VALUE)
@JvmField val largeNullableByteArray: DataType<ByteArray?> = Bytes(false, Int.MAX_VALUE)
*/
@Deprecated(bytesMessage, ReplaceWith("byteString"))
@JvmField val byteArray: DataType<ByteArray> = Bytes(false, Int.MAX_VALUE)

@Deprecated(bytesMessage, ReplaceWith("nullableByteString"))
@JvmField val nullableByteArray: DataType<ByteArray?> = Bytes(true, Int.MAX_VALUE)
