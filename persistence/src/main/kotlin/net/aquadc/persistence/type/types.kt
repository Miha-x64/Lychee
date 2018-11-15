package net.aquadc.persistence.type

import java.util.*


/**
 * Represents a way of storing a value.
 */
sealed class DataType<T>(
        @JvmField val isNullable: Boolean
) {

    /**
     * @param sizeBits can be 1, 8, 16, 32, or 64, and doesn't depend on [isNullable], i. e. nullability info is ignored
     */
    abstract class Integer<T>(
            isNullable: Boolean,
            @JvmField val sizeBits: Int
    ) : DataType<T>(isNullable) {

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

        // *ahem* https://youtrack.jetbrains.com/issue/KT-15595
        private companion object {
            @JvmField val intSizes = intArrayOf(1, 8, 16, 32, 64)
        }

    }

    /**
     * @param sizeBits can be 32 or 64, doesn't depend on [isNullable]
     */
    abstract class Floating<T>(
            isNullable: Boolean,
            @JvmField val sizeBits: Int
    ) : DataType<T>(isNullable) {

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
    abstract class Str<T>(
            isNullable: Boolean,
            @JvmField val maxLengthChars: Int
    ) : DataType<T>(isNullable) {

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
    abstract class Blob<T>(
            isNullable: Boolean,
            @JvmField val maxLength: Int
    ) : DataType<T>(isNullable) {

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
//    abstract class Dictionary<M, K, V> internal constructor(isNullable: Boolean, keyType: DataType<K>, valueType: DataType<K>) : DataType<M>(isNullable) TODO
//    abstract class Union<T> internal constructor(isNullable: Boolean, types: List<DataType<out T>>) : DataType<T>(isNullable) TODO
//    abstract class Struct<T> internal constructor(isNullable: Boolean, def: StructDef<T>) : DataType<T>(isNullable) TODO
//    Date is not supported because it's mutable and the most parts of it are deprecated 20+ years ago. TODO consider


}
