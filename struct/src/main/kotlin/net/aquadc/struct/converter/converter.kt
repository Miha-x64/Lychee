package net.aquadc.struct.converter

import android.database.Cursor
import android.database.sqlite.SQLiteStatement
import java.sql.PreparedStatement
import java.sql.ResultSet


/**
 * Represents an abstract way of converting a value of a single field of a struct instance.
 */
interface Converter<T> {
    val isNullable: Boolean
    val dataType: DataType
}

sealed class DataType {
    class Integer internal constructor(val sizeBits: Int, val signed: Boolean) : DataType()
    class Float internal constructor(val sizeBits: Int) : DataType()
    class String internal constructor(val maxLength: Int) : DataType()
    class Blob internal constructor(val maxLength: Int) : DataType()
}

/**
 * Describes supported basic data types.
 * Any converter must be able to describe its contents using [DataType],
 * so data transfer protocol implementations could be able to handle it.
 */
object DataTypes {
    val Bool = DataType.Integer(1, false)
    val Int8 = DataType.Integer(8, true)
    val Int16 = DataType.Integer(16, true)
    val Int32 = DataType.Integer(32, true)
    val Int64 = DataType.Integer(64, true)

    val Float32 = DataType.Float(32)
    val Float64 = DataType.Float(64)

    val SmallString = DataType.String(Byte.MAX_VALUE.toInt())
    val MediumString = DataType.String(Short.MAX_VALUE.toInt())
    val LargeString = DataType.String(Int.MAX_VALUE)

    val SmallBlob = DataType.Blob(Byte.MAX_VALUE.toInt())
    val MediumBlob = DataType.Blob(Short.MAX_VALUE.toInt())
    val LargeBlob = DataType.Blob(Int.MAX_VALUE)
}

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

/**
 * Cool converter supporting both JDBC and Android.
 */
interface UniversalConverter<T> : JdbcConverter<T>, AndroidSqliteConverter<T>

