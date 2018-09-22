package net.aquadc.persistence.converter

import android.database.Cursor
import android.database.sqlite.SQLiteStatement
import android.os.Parcel
import java.io.DataInput
import java.io.DataOutput
import java.sql.PreparedStatement
import java.sql.ResultSet

@PublishedApi internal open class EnumConverter<E : Enum<E>>(
        private val enumType: Class<E>,
        dataType: DataType
) : SimpleConverter<E>(dataType, false) {

    // JDBC

    final override fun bind(statement: PreparedStatement, index: Int, value: E) {
        string.bind(statement, index, asString(value))
    }

    final override fun get(resultSet: ResultSet, index: Int): E =
            lookup(string.get(resultSet, index))

    // Android SQLite

    final override fun bind(statement: SQLiteStatement, index: Int, value: E) {
        string.bind(statement, index, asString(value))
    }

    @Suppress("RedundantModalityModifier")
    open override fun asString(value: E): String = value.name

    final override fun get(cursor: Cursor, index: Int): E = lookup(cursor.getString(index))

    // IO streams

    final override fun write(output: DataOutput, value: E) {
        string.write(output, asString(value))
    }

    final override fun read(input: DataInput): E =
            lookup(string.read(input))

    // Parcel

    final override fun write(destination: Parcel, value: E) {
        string.write(destination, asString(value))
    }

    final override fun read(source: Parcel): E =
            lookup(string.read(source))

    // own

    protected open fun lookup(name: String): E = java.lang.Enum.valueOf<E>(enumType, name)

}
