package net.aquadc.persistence.converter

import android.database.Cursor
import android.database.sqlite.SQLiteStatement
import net.aquadc.persistence.stream.CleverDataInput
import net.aquadc.persistence.stream.CleverDataOutput
import net.aquadc.persistence.struct.t
import java.lang.Double.doubleToLongBits
import java.lang.Double.longBitsToDouble
import java.lang.Float.floatToIntBits
import java.lang.Float.intBitsToFloat
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types


internal abstract class SimpleConverter<T>(
        override val dataType: DataType,
        override val isNullable: Boolean
) : UniversalConverter<T>

private class BasicConverter<T>(
        private val javaType: Class<T>,
        dataType: DataType,
        isNullable: Boolean
) : SimpleConverter<T>(dataType, isNullable) {

    // JDBC

    override fun bind(statement: PreparedStatement, index: Int, value: T) {
        val i = 1 + index
        when (value) {
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

    // Android SQLite

    override fun bind(statement: SQLiteStatement, index: Int, value: T) {
        val i = 1 + index
        when (value) {
            is Boolean -> statement.bindLong(i, if (value) 1 else 0)
            is Byte, is Short, is Int, is Long -> statement.bindLong(i, (value as Number).toLong())
            is Float, is Double -> statement.bindDouble(i, (value as Number).toDouble())
            is String -> statement.bindString(i, value)
            is ByteArray -> statement.bindBlob(i, value)
            null -> statement.bindNull(i)
            else -> throw AssertionError()
        }
    }

    override fun asString(value: T): String =
            value.toString()

    @Suppress("IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST")
    override fun get(cursor: Cursor, index: Int): T = when (javaType) {
        t<Boolean>() -> cursor.getInt(index) != 0
        t<Byte>() -> {
            val sh = cursor.getShort(index)
            if (sh !in Byte.MIN_VALUE .. Byte.MAX_VALUE)
                throw IllegalStateException("value $sh cannot be fit into a byte")
            sh
        }
        t<Short>() -> cursor.getShort(index)
        t<Int>() -> cursor.getInt(index)
        t<Long>() -> cursor.getLong(index)
        t<Float>() -> cursor.getFloat(index)
        t<Double>() -> cursor.getDouble(index)
        t<String>() -> cursor.getString(index)
        t<ByteArray>() -> cursor.getBlob(index)
        else -> throw AssertionError()
    } as T

    // IO streams

    override fun write(output: CleverDataOutput, value: T) {
        when (javaType) {
            t<String>() -> return output.writeString(value as String?)
            t<ByteArray>() -> return output.writeBytes(value as ByteArray?)
            t<Boolean>() -> return output.writeByte(when (value as Boolean?) {
                null -> {
                    check(isNullable); -1
                }
                false -> 0
                true -> 1
            })
        }

        if (isNullable) {
            val isNull = value == null
            output.writeByte(if (isNull) -1 else 0)
            if (isNull) return
        }

        when (value) {
            is Byte -> output.writeByte(value.toInt())
            is Short -> output.writeShort(value.toInt())
            is Int -> output.writeInt(value)
            is Long -> output.writeLong(value)
            is Float -> output.writeInt(floatToIntBits(value))
            is Double -> output.writeLong(doubleToLongBits(value))
            else -> throw AssertionError()
        }
    }

    @Suppress("IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST")
    override fun read(input: CleverDataInput): T {
        when (javaType) {
            t<String>() -> return input.readString() as T
            t<ByteArray>() -> return input.readBytes() as T
            t<Boolean>() -> return when (input.readByte().toInt()) {
                -1 -> null
                0 -> false
                1 -> true
                else -> throw AssertionError()
            } as T
        }

        if (isNullable && input.readByte() == (-1).toByte()) {
            return null as T
        }

        return when (javaType) {
            t<Byte>() -> input.readByte()
            t<Short>() -> input.readShort()
            t<Int>() -> input.readInt()
            t<Long>() -> input.readLong()
            t<Float>() -> intBitsToFloat(input.readInt())
            t<Double>() -> longBitsToDouble(input.readLong())
            else -> throw AssertionError()
        } as T
    }

}

val bool: UniversalConverter<Boolean> = BasicConverter(t(), DataTypes.Bool, false)
val nullableBool: UniversalConverter<Boolean?> = BasicConverter(t(), DataTypes.Bool, true)

val byte: UniversalConverter<Byte> = BasicConverter(t(), DataTypes.Int8, false)
val nullableByte: UniversalConverter<Byte?> = BasicConverter(t(), DataTypes.Int8, true)

val short: UniversalConverter<Short> = BasicConverter(t(), DataTypes.Int16, false)
val nullableShort: UniversalConverter<Short?> = BasicConverter(t(), DataTypes.Int16, true)

val int: UniversalConverter<Int> = BasicConverter(t(), DataTypes.Int32, false)
val nullableInt: UniversalConverter<Int?> = BasicConverter(t(), DataTypes.Int32, true)

val long: UniversalConverter<Long> = BasicConverter(t(), DataTypes.Int64, false)
val nullableLong: UniversalConverter<Long?> = BasicConverter(t(), DataTypes.Int64, true)

val float: UniversalConverter<Float> = BasicConverter(t(), DataTypes.Float32, false)
val nullableFloat: UniversalConverter<Float?> = BasicConverter(t(), DataTypes.Float32, true)

val double: UniversalConverter<Double> = BasicConverter(t(), DataTypes.Float64, false)
val nullableDouble: UniversalConverter<Double?> = BasicConverter(t(), DataTypes.Float64, true)

val string: UniversalConverter<String> = BasicConverter(t(), DataTypes.LargeString, false)
val nullableString: UniversalConverter<String?> = BasicConverter(t(), DataTypes.LargeString, true)

@Deprecated("Note: if you mutate array, we won't notice — you must set() it in a transaction. " +
        "Consider using immutable ByteString instead.", ReplaceWith("byteString"))
val bytes: UniversalConverter<ByteArray> = BasicConverter(t(), DataTypes.LargeBlob, false)

@Deprecated("Note: if you mutate array, we won't notice — you must set() it in a transaction. " +
        "Consider using immutable ByteString instead.", ReplaceWith("nullableByteString"))
val nullableBytes: UniversalConverter<ByteArray?> = BasicConverter(t(), DataTypes.LargeBlob, true)


// Date is not supported because it's mutable and the most parts of it are deprecated 20+ years ago.
// TODO: To be considered...
