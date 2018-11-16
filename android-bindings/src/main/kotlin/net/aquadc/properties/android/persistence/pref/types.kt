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

// ^ commented out 'isNullable': we use default value even for nullable types

// NOTE: `null` means 'absent' here
@Suppress("UNCHECKED_CAST")
private fun <T> DataType<T>.get(prefs: SharedPreferences, key: String): T? {
    if (!prefs.contains(key)) return null

    val map = prefs.all // sadly, copying prefs fully is the only way to achieve correctness concurrently

    val value = map[key]
    if (value === null) return null

    return when (this) {
        is DataType.Simple<T> -> {
            decode(when (kind) {
                DataType.Simple.Kind.Bool -> value as Boolean
                DataType.Simple.Kind.I8 -> (value as Int).assertFitsByte()
                DataType.Simple.Kind.I16 -> (value as Int).assertFitsShort()
                DataType.Simple.Kind.I32 -> value as Int
                DataType.Simple.Kind.I64 -> value as Long
                DataType.Simple.Kind.F32 -> value as Float
                DataType.Simple.Kind.F64 -> java.lang.Double.longBitsToDouble(value as Long)
                DataType.Simple.Kind.Str -> value as String
                DataType.Simple.Kind.Blob -> Base64.decode(value as String, Base64.DEFAULT)
            })
        }
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
        is DataType.Simple<T> -> {
            val v = encode(value)
            when (kind) {
                DataType.Simple.Kind.Bool -> editor.putBoolean(key, v as Boolean).ignored
                DataType.Simple.Kind.I8 -> editor.putInt(key, (v as Byte).toInt()).ignored
                DataType.Simple.Kind.I16 -> editor.putInt(key, (v as Short).toInt()).ignored
                DataType.Simple.Kind.I32 -> editor.putInt(key, v as Int).ignored
                DataType.Simple.Kind.I64 -> editor.putLong(key, v as Long).ignored
                DataType.Simple.Kind.F32 -> editor.putFloat(key, v as Float).ignored
                DataType.Simple.Kind.F64 -> editor.putLong(key, java.lang.Double.doubleToLongBits(v as Double)).ignored
                DataType.Simple.Kind.Str -> editor.putString(key, v as String).ignored
                DataType.Simple.Kind.Blob -> editor.putString(key, Base64.encodeToString(v as ByteArray, Base64.DEFAULT)).ignored
            }.also { }
        }
    }

}

@Suppress("unused" /* receiver */)
private inline val Any?.ignored get() = Unit
