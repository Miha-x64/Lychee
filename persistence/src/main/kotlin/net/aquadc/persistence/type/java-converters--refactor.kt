package net.aquadc.persistence.type

import android.content.SharedPreferences
import android.database.Cursor
import android.database.sqlite.SQLiteStatement
import android.util.Base64
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
        t<Byte>() -> cursor.getShort(index).assertFitsByte()
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
            t<Boolean>() -> return output.writeByte((value as Boolean?).asByte().toInt())
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
            t<Boolean>() -> return input.readByte().asBoolean() as T
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

    // Prefs

    @Suppress("IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST")
    override fun get(prefs: SharedPreferences, key: String, default: T): T {
        if (isNullable) {
            when (javaType) {
                t<Boolean>() -> return prefs.getInt(key, (default as Boolean?).asByte().toInt()).toByte().asBoolean() as T
                t<Byte>() -> return prefs.getInt(key, (default as Byte?)?.toInt() ?: Int.MIN_VALUE)
                        .let { if (it == Int.MIN_VALUE) null else it.assertFitsByte() } as T
                t<Short>() -> return prefs.getInt(key, (default as Short?)?.toInt() ?: Int.MIN_VALUE)
                        .let { if (it == Int.MIN_VALUE) null else it.assertFitsShort() } as T
                t<Int>() -> return prefs.getLong(key, (default as Int?)?.toLong() ?: Long.MIN_VALUE)
                        .let { if (it == Long.MIN_VALUE) null else it.assertFitsInt() } as T
                t<Long>() -> return prefs.getString(key, (default as Long?).toString())
                        .let { if (it == "null") null else it.toLong() } as T
                t<Float>() -> return prefs.getLong(key, (default as Float?)?.let(::floatToIntBits)?.toLong() ?: Long.MIN_VALUE)
                        .let { if (it == Long.MIN_VALUE) null else it.assertFitsInt().let(::intBitsToFloat) } as T
                t<Double>() -> return prefs.getString(key, (default as Double?).toString())
                        .let { if (it == "null") null else it.toDouble() } as T
            }
        } else {
            when (javaType) {
                t<Boolean>() -> return prefs.getBoolean(key, default as Boolean) as T
                t<Byte>() -> return prefs.getInt(key, (default as Byte).toInt()).assertFitsByte() as T
                t<Short>() -> return prefs.getInt(key, (default as Short).toInt()).assertFitsShort() as T
                t<Int>() -> return prefs.getInt(key, default as Int) as T
                t<Long>() -> return prefs.getLong(key, default as Long) as T
                t<Float>() -> return prefs.getFloat(key, default as Float) as T
                t<Double>() -> return longBitsToDouble(prefs.getLong(key, doubleToLongBits(default as Double))) as T
            }
        }

        return when (javaType) {
            t<String>() -> prefs.getString(key, default as String?)
            t<ByteArray>() -> prefs.getString(key, (default as ByteArray?)?.let { Base64.encodeToString(it, Base64.DEFAULT) })
                    ?.let { Base64.decode(it, Base64.DEFAULT) }
            else -> throw AssertionError()
        } as T
    }

    override fun put(editor: SharedPreferences.Editor, key: String, value: T) {
        if (isNullable) {
            when (javaType) {
                t<Boolean>() -> return editor.putInt(key, (value as Boolean?).asByte().toInt()).asUnit()
                t<Byte>() -> return editor.putInt(key, (value as Byte?)?.toInt() ?: Int.MIN_VALUE).asUnit()
                t<Short>() -> return editor.putInt(key, (value as Short?)?.toInt() ?: Int.MIN_VALUE).asUnit()
                t<Int>() -> return editor.putLong(key, (value as Int?)?.toLong() ?: Long.MIN_VALUE).asUnit()
                t<Long>() -> return editor.putString(key, (value as Long?).toString()).asUnit()
                t<Float>() -> return editor.putLong(key, (value as Float?)?.let(::floatToIntBits)?.toLong() ?: Long.MIN_VALUE).asUnit()
                t<Double>() -> return editor.putString(key, (value as Double?).toString()).asUnit()
            }
        } else {
            when (javaType) {
                t<Boolean>() -> return editor.putBoolean(key, value as Boolean).asUnit()
                t<Byte>() -> return editor.putInt(key, (value as Byte).toInt()).asUnit()
                t<Short>() -> return editor.putInt(key, (value as Short).toInt()).asUnit()
                t<Int>() -> return editor.putInt(key, value as Int).asUnit()
                t<Long>() -> return editor.putLong(key, value as Long).asUnit()
                t<Float>() -> return editor.putFloat(key, value as Float).asUnit()
                t<Double>() -> return editor.putLong(key, doubleToLongBits(value as Double)).asUnit()
            }
        }

        return when (javaType) {
            t<String>() -> editor.putString(key, value as String?).asUnit()
            t<ByteArray>() -> editor.putString(key, (value as ByteArray?)?.let { Base64.encodeToString(it, Base64.DEFAULT) }).asUnit()
            else -> throw AssertionError()
        }
    }

    // util

    private fun Short.assertFitsByte(): Byte {
        if (this !in Byte.MIN_VALUE..Byte.MAX_VALUE)
            throw IllegalStateException("value ${this} cannot be fit into a byte")
        return toByte()
    }

    private fun Int.assertFitsByte(): Byte {
        if (this !in Byte.MIN_VALUE..Byte.MAX_VALUE)
            throw IllegalStateException("value ${this} cannot be fit into a byte")
        return toByte()
    }

    private fun Int.assertFitsShort(): Byte {
        if (this !in Short.MIN_VALUE..Short.MAX_VALUE)
            throw IllegalStateException("value ${this} cannot be fit into a short")
        return toByte()
    }

    private fun Long.assertFitsInt(): Int {
        if (this !in Int.MIN_VALUE..Int.MAX_VALUE)
            throw IllegalStateException("value ${this} cannot be fit into an int")
        return toInt()
    }

    private fun Boolean?.asByte(): Byte = when (this) {
        null -> {
            check(isNullable); -1
        }
        false -> 0
        true -> 1
    }

    private fun Byte.asBoolean(): Boolean? {
        return when (this) {
            (-1).toByte() -> null
            0.toByte() -> false
            1.toByte() -> true
            else -> throw AssertionError()
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun Any?.asUnit() = Unit

}



