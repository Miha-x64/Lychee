package net.aquadc.properties.android.persistence.pref

import android.content.SharedPreferences
import android.util.Base64
import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.type.DataType


internal fun <T> FieldDef<*, T>.get(prefs: SharedPreferences): T {
    val value = type.get(prefs, name)
    return if (value === null/* && !type.isNullable*/) default else value
}

internal fun <T> DataType<T>.get(prefs: SharedPreferences, name: String, default: T): T {
    val value = get(prefs, name)
    return if (value === null/* && !isNullable*/) default else value
}

// ^ commented out 'isNullable': we use default value even for nullable types. Fixme: is this correct?

// NOTE: `null` means 'absent' here
@Suppress("UNCHECKED_CAST")
private fun <T> DataType<T>.get(prefs: SharedPreferences, key: String): T? {
    if (!prefs.contains(key)) return null

    val map = prefs.all // sadly, copying prefs fully is the only way to aceive correctness concurrently

    val value = map[key]
    if (value === null) return null

    return when (this) {
        is DataType.Integer -> {
            @Suppress("IMPLICIT_CAST_TO_ANY")
            asT(when (sizeBits) {
                1 -> value as Boolean
                8 -> (value as Int).assertFitsByte()
                16 -> (value as Int).assertFitsShort()
                32 -> value as Int
                64 -> value as Long
                else -> throw AssertionError()
            })
        }
        is DataType.Floating -> {
            @Suppress("IMPLICIT_CAST_TO_ANY")
            asT(when (sizeBits) {
                32 -> value as Float
                64 -> java.lang.Double.longBitsToDouble(value as Long)
                else -> throw AssertionError()
            })
        }
        is DataType.Str -> asT(value as String)
        is DataType.Blob -> asT(Base64.decode(value as String, Base64.DEFAULT))
    }
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

internal fun <T> DataType<T>.put(editor: SharedPreferences.Editor, key: String, value: T) {
    if (value === null)
        return editor.remove(key).ignored

    when (this) {
        is DataType.Integer -> {
            val asNum = asNumber(value)
            when (sizeBits) {
                1 -> editor.putBoolean(key, asNum as Boolean).ignored
                8 -> editor.putInt(key, (asNum as Byte).toInt()).ignored
                16 -> editor.putInt(key, (asNum as Short).toInt()).ignored
                32 -> editor.putInt(key, asNum as Int).ignored
                64 -> editor.putLong(key, asNum as Long).ignored
                else -> throw AssertionError()
            }
        }
        is DataType.Floating -> {
            val asNum = asNumber(value)
            when (sizeBits) {
                32 -> editor.putFloat(key, asNum as Float).ignored
                64 -> editor.putLong(key, java.lang.Double.doubleToLongBits(asNum as Double)).ignored
                else -> throw AssertionError()
            }
        }
        is DataType.Str -> return editor.putString(key, asString(value)).ignored
        is DataType.Blob -> return editor.putString(key, Base64.encodeToString(asByteArray(value), Base64.DEFAULT)).ignored
    }

}

@Suppress("unused" /* receiver */)
private inline val Any?.ignored get() = Unit
