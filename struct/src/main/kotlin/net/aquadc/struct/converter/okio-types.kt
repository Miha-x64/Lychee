package net.aquadc.struct.converter

import okio.ByteString
import java.sql.PreparedStatement
import java.sql.ResultSet


private class ByteStringConverter(
        isNullable: Boolean
) : SimpleConverter<ByteString?>(DataTypes.LargeBlob, isNullable) {

    override fun bind(statement: PreparedStatement, index: Int, value: ByteString?) {
        statement.setBytes(1 + index, value?.toByteArray())
    }

    override fun get(resultSet: ResultSet, index: Int): ByteString? =
            resultSet.getBytes(1 + index)?.let { ByteString.of(it, 0, it.size) }

    override fun toString(value: ByteString?): String =
            TODO("BLOB in Android SQLite?")

    override fun get(cursor: Nothing, index: Int): ByteString? =
            cursor

}

@Suppress("UNCHECKED_CAST")
val byteString: UniversalConverter<ByteString> = ByteStringConverter(false) as UniversalConverter<ByteString>
val nullableByteString: UniversalConverter<ByteString?> = ByteStringConverter(true)
