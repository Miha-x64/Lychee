@file:Suppress("PublicApiImplicitType")
package net.aquadc.persistence.converter

import android.content.SharedPreferences
import android.database.Cursor
import android.database.sqlite.SQLiteStatement
import net.aquadc.persistence.stream.CleverDataInput
import net.aquadc.persistence.stream.CleverDataOutput
import java.sql.PreparedStatement
import java.sql.ResultSet

/**
 * Stores [T] values as [U] ones.
 */
abstract class DelegatingConverter<T, U>(
        private val converter: Converter<U>
) : UniversalConverter<T> {

    override val isNullable: Boolean
        get() = converter.isNullable

    override val dataType: DataType
        get() = converter.dataType

    // JDBC

    override fun bind(statement: PreparedStatement, index: Int, value: T) =
            (converter as JdbcConverter<U>).bind(statement, index, value.to())

    override fun get(resultSet: ResultSet, index: Int): T =
            from((converter as JdbcConverter<U>).get(resultSet, index))

    // Android SQLite

    override fun bind(statement: SQLiteStatement, index: Int, value: T) =
            (converter as AndroidSqliteConverter<U>).bind(statement, index, value.to())

    // asString skipped — must be implemented by subclasses

    override fun get(cursor: Cursor, index: Int): T =
            from((converter as AndroidSqliteConverter<U>).get(cursor, index))

    // IO streams

    override fun write(output: CleverDataOutput, value: T) =
            (converter as DataIoConverter<U>).write(output, value.to())

    override fun read(input: CleverDataInput): T =
            from((converter as DataIoConverter<U>).read(input))

    // prefs

    override fun get(prefs: SharedPreferences, key: String, default: T): T =
            from((converter as PrefsConverter<U>).get(prefs, key, default.to()))

    override fun put(editor: SharedPreferences.Editor, key: String, value: T) =
            (converter as PrefsConverter<U>).put(editor, key, value.to())

    // own

    /**
     * Creates a value of [T] from a [U] [value].
     */
    protected abstract fun from(value: U): T

    /**
     * Converts a [T] value to a [U] one.
     */
    protected abstract fun T.to(): U

}
