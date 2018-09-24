package net.aquadc.persistence.converter

import okio.ByteString


private class ByteStringConverter(
        isNullable: Boolean
) : DelegatingConverter<ByteString?, ByteArray?>(if (isNullable) nullableBytes else bytes as Converter<ByteArray?>) {

    override fun asString(value: ByteString?): String =
            TODO("BLOB in Android SQLite?")

    override fun from(value: ByteArray?): ByteString? =
            value?.let { ByteString.of(value, 0, value.size) }

    override fun ByteString?.to(): ByteArray? =
            this?.toByteArray()

}

@Suppress("UNCHECKED_CAST")
val byteString: UniversalConverter<ByteString> = ByteStringConverter(false) as UniversalConverter<ByteString>
val nullableByteString: UniversalConverter<ByteString?> = ByteStringConverter(true)
