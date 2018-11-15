@file:JvmName("ArrayTypes")
package net.aquadc.persistence.type


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
@JvmField val smallNullableString: DataType<String?> = Strings(true, Byte.MAX_VALUE.toInt())

@JvmField val mediumString: DataType<String> = Strings(false, Short.MAX_VALUE.toInt())
@JvmField val mediumNullableString: DataType<String?> = Strings(true, Short.MAX_VALUE.toInt())

@JvmField val largeString: DataType<String> = Strings(false, Int.MAX_VALUE)
@JvmField val largeNullableString: DataType<String?> = Strings(true, Int.MAX_VALUE)
*/
@JvmField val string: DataType<String> = Strings(false, Int.MAX_VALUE)
@JvmField val nullableString: DataType<String?> = Strings(true, Int.MAX_VALUE)


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
@JvmField val smallNullableByteArray: DataType<ByteArray?> = Bytes(true, Byte.MAX_VALUE.toInt())

@JvmField val mediumByteArray: DataType<ByteArray> = Bytes(false, Short.MAX_VALUE.toInt())
@JvmField val mediumNullableByteArray: DataType<ByteArray?> = Bytes(true, Short.MAX_VALUE.toInt())

@JvmField val largeByteArray: DataType<ByteArray> = Bytes(false, Int.MAX_VALUE)
@JvmField val largeNullableByteArray: DataType<ByteArray?> = Bytes(true, Int.MAX_VALUE)
*/
@Deprecated(bytesMessage, ReplaceWith("byteString"))
@JvmField val byteArray: DataType<ByteArray> = Bytes(false, Int.MAX_VALUE)

@Deprecated(bytesMessage, ReplaceWith("nullableByteString"))
@JvmField val nullableByteArray: DataType<ByteArray?> = Bytes(true, Int.MAX_VALUE)
