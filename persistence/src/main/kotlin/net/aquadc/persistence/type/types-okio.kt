package net.aquadc.persistence.type

import okio.ByteString


private class ByteStr<T : ByteString?>(
        isNullable: Boolean, maxLength: Int
) : DataType.Blob<T>(isNullable, maxLength) {

    override fun asByteArray(value: T): ByteArray =
            if (value === null) throw NullPointerException()
            else value.toByteArray()

    @Suppress("UNCHECKED_CAST")
    override fun asT(value: ByteArray): T =
            value as T

}

/*
@JvmField val smallByteString: DataType<ByteString> = ByteStr(false, Byte.MAX_VALUE.toInt())
@JvmField val smallNullableByteString: DataType<ByteString?> = ByteStr(true, Byte.MAX_VALUE.toInt())

@JvmField val mediumByteString: DataType<ByteString> = ByteStr(false, Short.MAX_VALUE.toInt())
@JvmField val mediumNullableByteString: DataType<ByteString?> = ByteStr(true, Short.MAX_VALUE.toInt())

@JvmField val largeByteString: DataType<ByteString> = ByteStr(false, Int.MAX_VALUE)
@JvmField val largeNullableByteString: DataType<ByteString?> = ByteStr(true, Int.MAX_VALUE)
*/
@JvmField val byteString: DataType<ByteString> = ByteStr(false, Int.MAX_VALUE)
@JvmField val nullableByteString: DataType<ByteString?> = ByteStr(true, Int.MAX_VALUE)
