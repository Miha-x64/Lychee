@file:JvmName("OkioTypes")
package net.aquadc.persistence.type

import okio.ByteString


private class ByteStr : DataType.Simple<Any?>(DataType.Simple.Kind.Blob) {

    override fun decode(value: Any?): Any? =
            ByteString.of(value as ByteArray, 0, value.size)

    override fun encode(value: Any?): Any =
            (value as ByteString).toByteArray()

}

@Suppress("UNCHECKED_CAST")
@JvmField val byteString: DataType<ByteString> = ByteStr() as DataType<ByteString>
