@file:JvmName("OkioTypes")
package net.aquadc.persistence.type

import okio.ByteString


private class ByteStr<T>(
        isNullable: Boolean, kind: Kind
) : DataType.Simple<T>(isNullable, kind) {

    override fun decode(value: Any): T =
            ByteString.of(value as ByteArray, 0, value.size) as T

    override fun encode(value: T): Any =
            (value as ByteString).toByteArray()

}

/*
@JvmField val smallByteString: DataType<ByteString> = ByteStr(false, DataType.Simple.Kind.TinyBlob)
@JvmField val smallNullableByteString: DataType<ByteString?> = ByteStr(true, DataType.Simple.Kind.TinyBlob)

@JvmField val mediumByteString: DataType<ByteString> = ByteStr(false, DataType.Simple.Kind.Blob)
@JvmField val mediumNullableByteString: DataType<ByteString?> = ByteStr(true, DataType.Simple.Kind.Blob)

@JvmField val largeByteString: DataType<ByteString> = ByteStr(false, DataType.Simple.Kind.BigBlob)
@JvmField val largeNullableByteString: DataType<ByteString?> = ByteStr(true, DataType.Simple.Kind.BigBlob)
*/
@JvmField val byteString: DataType<ByteString> = ByteStr(false, DataType.Simple.Kind.Blob)
@JvmField val nullableByteString: DataType<ByteString?> = ByteStr(true, DataType.Simple.Kind.Blob)
