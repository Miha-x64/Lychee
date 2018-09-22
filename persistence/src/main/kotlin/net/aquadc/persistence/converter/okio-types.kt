package net.aquadc.persistence.converter

import android.database.Cursor
import android.database.sqlite.SQLiteStatement
import android.os.Parcel
import okio.ByteString
import java.io.DataInput
import java.io.DataOutput
import java.sql.PreparedStatement
import java.sql.ResultSet


private class ByteStringConverter(
        isNullable: Boolean
) : SimpleConverter<ByteString?>(DataTypes.LargeBlob, isNullable) {

    // JDBC

    override fun bind(statement: PreparedStatement, index: Int, value: ByteString?) {
        statement.setBytes(1 + index, value?.toByteArray())
    }

    override fun get(resultSet: ResultSet, index: Int): ByteString? =
            resultSet.getBytes(1 + index)?.let { ByteString.of(it, 0, it.size) }


    // Android SQLite

    override fun bind(statement: SQLiteStatement, index: Int, value: ByteString?) {
        statement.bindBlob(1 + index, value?.toByteArray())
    }

    override fun asString(value: ByteString?): String =
            TODO("BLOB in Android SQLite?")

    override fun get(cursor: Cursor, index: Int): ByteString? =
            cursor.getBlob(index)?.let { ByteString.of(it, 0, it.size) }

    // IO streams

    override fun write(output: DataOutput, value: ByteString?) {
        nullableBytes.write(output, value?.toByteArray())
    }

    override fun read(input: DataInput): ByteString? =
            nullableBytes.read(input)?.let { ByteString.of(it, 0, it.size) }

    // Parcel

    override fun write(destination: Parcel, value: ByteString?) {
        nullableBytes.write(destination, value?.toByteArray())
    }

    override fun read(source: Parcel): ByteString? =
            nullableBytes.read(source)?.let { ByteString.of(it, 0, it.size) }

}

@Suppress("UNCHECKED_CAST")
val byteString: UniversalConverter<ByteString> = ByteStringConverter(false) as UniversalConverter<ByteString>
val nullableByteString: UniversalConverter<ByteString?> = ByteStringConverter(true)
