package net.aquadc.properties.sql.dialect.sqlite

import net.aquadc.properties.sql.Converter
import net.aquadc.properties.sql.t
import java.lang.AssertionError
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types


internal abstract class SimpleConverter<T>(
        override val javaType: Class<out T>,
        override val sqlType: String,
        override val isNullable: Boolean
) : Converter<T>

private class BasicConverter<T>(
        javaType: Class<out T>,
        sqlType: String,
        isNullable: Boolean
) : SimpleConverter<T>(javaType, sqlType, isNullable) {

    override fun bind(statement: PreparedStatement, index: Int, value: T) {
        val i = 1 + index
        return when (value) {
            is Boolean, is Short, is Int, is Long, is Float, is Double,
            is String,
            is ByteArray -> statement.setObject(i, value)
            is Byte -> statement.setInt(i, value.toInt())
            null -> statement.setNull(i, Types.NULL)
            else -> throw AssertionError()
        }
    }

    override fun get(resultSet: ResultSet, index: Int): T {
        val i = 1 + index
        @Suppress("IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST")
        return when (javaType) {
            t<Boolean>() -> resultSet.getBoolean(i)
            t<Byte>() -> resultSet.getByte(i)
            t<Short>() -> resultSet.getShort(i)
            t<Int>() -> resultSet.getInt(i)
            t<Long>() -> resultSet.getLong(i)
            t<Float>() -> resultSet.getFloat(i)
            t<Double>() -> resultSet.getDouble(i)
            t<String>() -> resultSet.getString(i)
            t<ByteArray>() -> resultSet.getBytes(i)
            else -> throw AssertionError()
        } as T
    }

}

val bool: Converter<Boolean> = BasicConverter(t<Boolean>(), "INTEGER", false)
val nullableBool: Converter<Boolean?> = BasicConverter(t<Boolean>(), "INTEGER", true)

val byte: Converter<Byte> = BasicConverter(t<Byte>(), "INTEGER", false)
val nullableByte: Converter<Byte?> = BasicConverter(t<Byte>(), "INTEGER", true)

val short: Converter<Short> = BasicConverter(t<Short>(), "INTEGER", false)
val nullableShort: Converter<Short?> = BasicConverter(t<Short>(), "INTEGER", true)

val int: Converter<Int> = BasicConverter(t<Int>(), "INTEGER", false)
val nullableInt: Converter<Int?> = BasicConverter(t<Int>(), "INTEGER", true)

val long: Converter<Long> = BasicConverter(t<Long>(), "INTEGER", false)
val nullableLong: Converter<Long?> = BasicConverter(t<Long>(), "INTEGER", true)

val float: Converter<Float> = BasicConverter(t<Float>(), "REAL", false)
val nullableFloat: Converter<Float?> = BasicConverter(t<Float>(), "REAL", true)

val double: Converter<Double> = BasicConverter(t<Double>(), "REAL", false)
val nullableDouble: Converter<Double?> = BasicConverter(t<Double>(), "REAL", true)

val string: Converter<String> = BasicConverter(t<String>(), "TEXT", false)
val nullableString: Converter<String?> = BasicConverter(t<String>(), "TEXT", true)

@Deprecated("Note: if you mutate array, we won't notice — you must set() it in a transaction. " +
        "Consider using immutable ByteString instead.", ReplaceWith("byteString"))
val bytes: Converter<ByteArray> = BasicConverter(t<ByteArray>(), "BLOB", false)

@Deprecated("Note: if you mutate array, we won't notice — you must set() it in a transaction. " +
        "Consider using immutable ByteString instead.", ReplaceWith("nullableByteString"))
val nullableBytes: Converter<ByteArray?> = BasicConverter(t<ByteArray>(), "BLOB", true)

// TODO: Date, etc
