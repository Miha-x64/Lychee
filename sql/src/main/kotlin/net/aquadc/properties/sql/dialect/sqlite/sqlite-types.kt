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

private class NumberConverter<T>(
        javaType: Class<out T>,
        sqlType: String,
        isNullable: Boolean
) : SimpleConverter<T>(javaType, sqlType, isNullable) {

    override fun bind(statement: PreparedStatement, index: Int, value: T) {
        val i = 1 + index
        return when (value) {
            is Boolean -> statement.setBoolean(i, value)
            is Byte,
            is Short -> statement.setInt(i, (value as Number).toInt())
            is Int -> statement.setInt(i, value)
            is Long -> statement.setLong(i, value)
            is Float -> statement.setFloat(i, value)
            is Double -> statement.setDouble(i, value)
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
            else -> throw AssertionError()
        } as T
    }

}

val bool: Converter<Boolean> = NumberConverter(t<Boolean>(), "INTEGER", false)
val nullableBool: Converter<Boolean?> = NumberConverter(t<Boolean>(), "INTEGER", true)

val byte: Converter<Byte> = NumberConverter(t<Byte>(), "INTEGER", false)
val nullableByte: Converter<Byte?> = NumberConverter(t<Byte>(), "INTEGER", true)

val short: Converter<Short> = NumberConverter(t<Short>(), "INTEGER", false)
val nullableShort: Converter<Short?> = NumberConverter(t<Short>(), "INTEGER", true)

val int: Converter<Int> = NumberConverter(t<Int>(), "INTEGER", false)
val nullableInt: Converter<Int?> = NumberConverter(t<Int>(), "INTEGER", true)

val long: Converter<Long> = NumberConverter(t<Long>(), "INTEGER", false)
val nullableLong: Converter<Long?> = NumberConverter(t<Long>(), "INTEGER", true)

val float: Converter<Float> = NumberConverter(t<Float>(), "REAL", false)
val nullableFloat: Converter<Float?> = NumberConverter(t<Float>(), "REAL", true)

val double: Converter<Double> = NumberConverter(t<Double>(), "REAL", false)
val nullableDouble: Converter<Double?> = NumberConverter(t<Double>(), "REAL", true)


private class StringConverter(
        isNullable: Boolean
) : SimpleConverter<String?>(t<String>(), "TEXT", isNullable) {

    override fun bind(statement: PreparedStatement, index: Int, value: String?) {
        statement.setString(1 + index, value)
    }

    override fun get(resultSet: ResultSet, index: Int): String? =
            resultSet.getString(1 + index)

}

val string: Converter<String> = StringConverter(false) as Converter<String>
val nullableString: Converter<String?> = StringConverter(false)


private class BytesConverter(
        isNullable: Boolean
) : SimpleConverter<ByteArray?>(ByteArray::class.java, "BLOB", isNullable) {

    override fun bind(statement: PreparedStatement, index: Int, value: ByteArray?) {
        statement.setBytes(1 + index, value)
    }

    override fun get(resultSet: ResultSet, index: Int): ByteArray? =
            resultSet.getBytes(1 + index)

}

@Deprecated("Note: if you mutate array, we won't notice — you must set() it in a transaction. " +
        "Consider using immutable ByteString instead.", ReplaceWith("byteString"))
val bytes: Converter<ByteArray> = BytesConverter(false) as Converter<ByteArray>

@Deprecated("Note: if you mutate array, we won't notice — you must set() it in a transaction. " +
        "Consider using immutable ByteString instead.", ReplaceWith("nullableByteString"))
val nullableBytes: Converter<ByteArray?> = BytesConverter(true)

// TODO: Date, etc
