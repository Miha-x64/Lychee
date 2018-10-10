package net.aquadc.persistence.type

import android.database.Cursor
import android.database.sqlite.SQLiteStatement
import java.sql.PreparedStatement
import java.sql.ResultSet


/**
 * A bridge between JDBC types and Java/Kotlin types.
 */
interface JdbcConverter<T> : Converter<T> {

    /**
     * @param index is 0-based
     */
    fun bind(statement: PreparedStatement, index: Int, value: T)

    /**
     * @param index is 0-based
     */
    fun get(resultSet: ResultSet, index: Int): T

}

/**
 * A bridge between SQLite types and Java/Kotlin types.
 */
interface AndroidSqliteConverter<T> : Converter<T> {

    /**
     * @param index is 1-based
     */
    fun bind(statement: SQLiteStatement, index: Int, value: T)

    /**
     * String representation used in `selectionArgs` in SQLite queries.
     */
    fun asString(value: T): String

    /**
     * @param index is 0-based
     */
    fun get(cursor: Cursor, index: Int): T

}
