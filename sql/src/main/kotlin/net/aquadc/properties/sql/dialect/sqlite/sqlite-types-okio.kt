package net.aquadc.properties.sql.dialect.sqlite

import net.aquadc.properties.sql.Converter
import okio.ByteString
import java.sql.PreparedStatement
import java.sql.ResultSet


private class ByteStringConverter(
        isNullable: Boolean
) : SimpleConverter<ByteString?>(ByteString::class.java, "BLOB", isNullable) {

    override fun bind(statement: PreparedStatement, index: Int, value: ByteString?) {
        statement.setBytes(1 + index, value?.toByteArray())
    }

    override fun get(resultSet: ResultSet, index: Int): ByteString? =
            resultSet.getBytes(1 + index)?.let { ByteString.of(it, 0, it.size) }

}

@Suppress("UNCHECKED_CAST")
val byteString: Converter<ByteString> = ByteStringConverter(false) as Converter<ByteString>
val nullableByteString: Converter<ByteString?> = ByteStringConverter(true)
