package net.aquadc.persistence.converter

import android.database.Cursor
import android.database.sqlite.SQLiteStatement
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


    override fun bind(statement: SQLiteStatement, index: Int, value: ByteString?) {
        statement.bindBlob(1 + index, value?.toByteArray())
    }

    override fun asString(value: ByteString?): String =
            TODO("BLOB in Android SQLite?")

    override fun get(cursor: Cursor, index: Int): ByteString? =
            cursor.getBlob(index)?.let { ByteString.of(it, 0, it.size) }

}

@Suppress("UNCHECKED_CAST")
val byteString: UniversalConverter<ByteString> = ByteStringConverter(false) as UniversalConverter<ByteString>
val nullableByteString: UniversalConverter<ByteString?> = ByteStringConverter(true)
