@file:JvmName("OkioTypes")
package net.aquadc.persistence.type

import okio.ByteString


@PublishedApi internal class ByteStr(
        private val blob: DataType.Simple<ByteArray>
) : DataType.Simple<Any?>(DataType.Simple.Kind.Blob) {

    override fun decode(value: Any?): Any? =
            blob.decode(value).let { ByteString.of(it, 0, it.size) }

    override fun encode(value: Any?): Any? =
            blob.encode((value as ByteString).toByteArray())

}

/**
 * Describes [ByteString] instances.
 */
@Suppress("UNCHECKED_CAST")
@JvmField val byteString: DataType<ByteString> = byteString(byteArray)

/**
 * Wraps a [ByteArray] [DataType] to achieve mutation-safety.
 */
@Suppress(
        "NOTHING_TO_INLINE", // single constructor call is a great candidate for inlining
        "UNCHECKED_CAST" // ByteStr is erased to avoid bridge methods
)
inline fun byteString(blobType: DataType.Simple<ByteArray>): DataType<ByteString> =
        ByteStr(blobType) as DataType<ByteString>
