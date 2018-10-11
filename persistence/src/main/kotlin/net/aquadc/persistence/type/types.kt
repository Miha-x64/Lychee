package net.aquadc.persistence.type

import java.util.*


/**
 * Represents a way of storing a value.
 */
sealed class DataType<T>(
        val isNullable: Boolean
) {

    private companion object {
        private val intSizes = intArrayOf(1, 8, 16, 32, 64)
    }

    /**
     * @param sizeBits can be 1, 8, 16, 32, or 64, and doesn't depend on [isNullable], i. e. nullability info is ignored
     */
    abstract class Integer<T> internal constructor(isNullable: Boolean, val sizeBits: Int) : DataType<T>(isNullable) {

        init {
            check(Arrays.binarySearch(intSizes, sizeBits) >= 0) { "invalid integer number size: $sizeBits bits" }
        }

        /**
         * @return [value] as a boxed primitive type depending on [sizeBits]: [Boolean], [Byte], [Short], [Int], or [Long]
         * @throws NullPointerException if [value] is null.
         */
        abstract fun asNumber(value: T): Any

        /**
         * @return [value] as [T]
         * @throws NullPointerException if [value] is not in [Boolean], [Byte], [Short], [Int], [Long]
         */
        abstract fun asT(value: Any): T

    }

    /**
     * @param sizeBits can be 32 or 64, doesn't depend on [isNullable]
     */
    abstract class Floating<T> internal constructor(isNullable: Boolean, val sizeBits: Int) : DataType<T>(isNullable) {

        init {
            check(sizeBits == 32 || sizeBits == 64) { "invalid floating-point number size: $sizeBits bits" }
        }

        /**
         * @return [value] as a [Double] or [Float]
         * @throws NullPointerException if [value] is `null`
         */
        abstract fun asNumber(value: T): Number

        /**
         * @return [value] as [T]
         */
        abstract fun asT(value: Number): T

    }

    /**
     * @param maxLengthChars can be [Byte.MAX_VALUE], [Short.MAX_VALUE], or [Int.MAX_VALUE], and used in SQL
     */
    abstract class Str<T> internal constructor(isNullable: Boolean, val maxLengthChars: Int) : DataType<T>(isNullable) {

        /**
         * @return [value] as a [String]
         * @throws NullPointerException if [value] is `null`
         */
        abstract fun asString(value: T): String

        /**
         * @return [value] as [T]
         */
        abstract fun asT(value: String): T

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

//    abstract class Collect<C : Collection<E>, E> internal constructor(isNullable: Boolean, eType: DataType<E>) : DataType<C>(isNullable) TODO
//    abstract class Map<M, K, V> internal constructor(isNullable: Boolean, keyType: DataType<K>, valueType: DataType<K>) : DataType<M>(isNullable) TODO
//    abstract class Union<T> internal constructor(isNullable: Boolean, types: List<DataType<out T>>) : DataType<T>(isNullable) TODO
//    abstract class Struct<T> internal constructor(isNullable: Boolean, def: StructDef<T>) : DataType<T>(isNullable) TODO
//    Date is not supported because it's mutable and the most parts of it are deprecated 20+ years ago. TODO consider


}

private class Ints<T>(isNullable: Boolean, sizeBits: Int) : DataType.Integer<T>(isNullable, sizeBits) {

    /**
     * {@implNote does nothing but sanity checks}
     */
    override fun asNumber(value: T): Any {
        if (value === null) throw NullPointerException()
        return when (sizeBits) {
            1 -> value as Boolean
            8 -> value as Byte
            16 -> value as Short
            32 -> value as Int
            64 -> value as Long
            else -> throw AssertionError()
        }
    }

    /**
     * {@implNote does nothing but sanity checks}
     */
    @Suppress("IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST")
    override fun asT(value: Any): T = when (sizeBits) {
        1 -> value as Boolean
        8 -> value as Byte
        16 -> value as Short
        32 -> value as Int
        64 -> value as Long
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


private class Floats<T>(isNullable: Boolean, sizeBits: Int) : DataType.Floating<T>(isNullable, sizeBits) {

    /**
     * {@implNote does nothing but sanity checks}
     */
    override fun asNumber(value: T): Number {
        if (value === null) throw NullPointerException()
        return when (sizeBits) {
            32 -> value as Float
            64 -> value as Double
            else -> throw AssertionError()
        }
    }

    /**
     * {@implNote does nothing but sanity checks}
     */
    @Suppress("IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST")
    override fun asT(value: Number): T = when (sizeBits) {
        32 -> value as Float
        64 -> value as Double
        else -> throw AssertionError()
    } as T

}

@JvmField val float: DataType<Float> = Floats(false, 32)
@JvmField val nullableFloat: DataType<Float?> = Floats(true, 32)

@JvmField val double: DataType<Double> = Floats(false, 64)
@JvmField val nullableDouble: DataType<Double?> = Floats(true, 64)


private class Strings<T>(isNullable: Boolean, maxLengthChars: Int) : DataType.Str<T>(isNullable, maxLengthChars) {

    /**
     * {@implNote does nothing but sanity checks}
     */
    override fun asString(value: T): String =
            if (value === null) throw NullPointerException() else value as String

    /**
     * {@implNote does nothing but sanity checks}
     */
    @Suppress("UNCHECKED_CAST")
    override fun asT(value: String): T =
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

    /**
     * {@implNote does nothing but sanity checks}
     */
    override fun asByteArray(value: T): ByteArray =
            if (value === null) throw NullPointerException() else value as ByteArray

    /**
     * {@implNote does nothing but sanity checks}
     */
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
