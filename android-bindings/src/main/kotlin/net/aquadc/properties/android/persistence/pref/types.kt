package net.aquadc.properties.android.persistence.pref

import android.content.SharedPreferences
import android.util.Base64
import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.match
import net.aquadc.properties.android.persistence.assertFitsByte
import net.aquadc.properties.android.persistence.assertFitsShort
import net.aquadc.properties.internal.Unset
import java.util.EnumSet


internal fun <T> FieldDef<*, T>.get(prefs: SharedPreferences): T {
    val value = type.get(prefs, name)
    return if (value === Unset) default else value
}

internal fun <T> DataType<T>.get(prefs: SharedPreferences, name: String, default: T): T {
    val value = get(prefs, name)
    return if (value === Unset) default else value
}

/**
 * SharedPrefs do not support storing null values. Null means 'absent' in this context.
 * To preserve consistent behaviour of default field values amongst nullable and non-nullable fields,
 * we store 'null' ourselves. If a field has String type, 'null' is stored as Boolean 'false'.
 * Otherwise 'null' is stored as a String "null".
 */
private val storedAsString = EnumSet.of(DataType.Simple.Kind.Str, DataType.Simple.Kind.Blob)

@Suppress("UNCHECKED_CAST")
private fun <T> DataType<T>.get(prefs: SharedPreferences, key: String): T {
    if (!prefs.contains(key)) return Unset as T

    val map = prefs.all // sadly, copying prefs fully is the only way to achieve correctness concurrently

    val value = map[key]
    if (value === null) return Unset as T

    return match { isNullable, simple ->
        if (isNullable && if (simple.kind in storedAsString) value == false else value == "null")
            return decode(null)

        decode(when (simple.kind) {
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

internal fun <T> DataType<T>.put(editor: SharedPreferences.Editor, key: String, value: T) {
    match { isNullable, simple ->
        if (isNullable && value === null) {
            if (value in storedAsString) editor.putBoolean(key, false)
            else editor.putString(key, "null")
            return
        }

        val v = encode(value)
        when (simple.kind) {
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

@Suppress("unused" /* receiver */)
private inline val Any?.ignored get() = Unit
