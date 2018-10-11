package net.aquadc.persistence.type

import android.content.SharedPreferences
import android.util.Base64
import net.aquadc.persistence.struct.t
import java.lang.Double.doubleToLongBits
import java.lang.Double.longBitsToDouble
import java.lang.Float.floatToIntBits
import java.lang.Float.intBitsToFloat


internal abstract class SimpleConverter<T>(
        override val dataType: DataType,
        override val isNullable: Boolean
) : UniversalConverter<T>

private class BasicConverter<T>(
        private val javaType: Class<T>,
        dataType: DataType,
        isNullable: Boolean
) : SimpleConverter<T>(dataType, isNullable) {

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



