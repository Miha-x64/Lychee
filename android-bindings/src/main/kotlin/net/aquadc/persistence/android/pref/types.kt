package net.aquadc.persistence.android.pref

import android.content.SharedPreferences
import android.util.Base64
import net.aquadc.collections.contains
import net.aquadc.collections.plus
import net.aquadc.persistence.fatMapTo
import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.serialized
import net.aquadc.properties.internal.Unset
import java.lang.Double as JavaLangDouble


internal fun <SCH : Schema<SCH>, T> SCH.get(what: FieldDef<SCH, T, *>, prefs: SharedPreferences): T {
    val value = (what as FieldDef<SCH, T, DataType<T>>).type.get(prefs, what.name.toString())
    return if (value !== Unset) value else
        defaultOrElse(what) { throw NoSuchElementException(what.toString()/* no value in shared prefs and no default */) }
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
@JvmSynthetic internal val storedAsString = DataType.NotNull.Simple.Kind.Str + DataType.NotNull.Simple.Kind.Blob

@Suppress("UNCHECKED_CAST")
private fun <T> DataType<T>.get(prefs: SharedPreferences, key: String): T {
    if (!prefs.contains(key)) return Unset as T

    val map = prefs.all // sadly, copying prefs fully is the only way to achieve correctness concurrently

    val value = map[key]
    if (value === null) return Unset as T

    val type = if (this is DataType.Nullable<*, *>) {
        val act = actualType
        when (act) {
            is DataType.Nullable<*, *> -> throw AssertionError()
            is DataType.NotNull.Simple<*> -> if (value == (if (act.kind in storedAsString) false else "null")) return null as T
            is DataType.NotNull.Collect<*, *, *>, is DataType.NotNull.Partial<*, *> -> if (value == false) return null as T
        }
        act as DataType<T/*!!*/>
    } else this

    return when (type) {
        is DataType.Nullable<*, *> -> throw AssertionError()
        is DataType.NotNull.Simple -> type.load(
            if (type.hasStringRepresentation) value as CharSequence
            else when (type.kind) {
                DataType.NotNull.Simple.Kind.Bool -> value as Boolean
                DataType.NotNull.Simple.Kind.I32 -> value as Int
                DataType.NotNull.Simple.Kind.I64 -> value as Long
                DataType.NotNull.Simple.Kind.F32 -> value as Float
                DataType.NotNull.Simple.Kind.F64 -> JavaLangDouble.longBitsToDouble(value as Long)
                DataType.NotNull.Simple.Kind.Str -> value as String
                DataType.NotNull.Simple.Kind.Blob -> Base64.decode(value as String, Base64.DEFAULT)
                else -> throw AssertionError()
            }
        )
        is DataType.NotNull.Collect<T, *, *> -> type.elementType.let { elementType ->
            if (elementType is DataType.NotNull.Simple<*> &&
                (elementType.hasStringRepresentation || elementType.kind == DataType.NotNull.Simple.Kind.Str)) // TODO should store everything in strings
                type.load((value as Set<String>).map(elementType::load)) // todo zero-copy
            else /* here we have a Collection<Whatever>, including potentially a collection of collections, structs, etc */
                serialized(type).load(Base64.decode(value as String, Base64.DEFAULT))
        }
        is DataType.NotNull.Partial<*, *> -> serialized(type).load(Base64.decode(value as String, Base64.DEFAULT))
    }
}

internal fun <T> DataType<T>.put(editor: SharedPreferences.Editor, key: String, value: T) {
    val type = if (this is DataType.Nullable<*, *>) {
        val act = actualType
        if (value == null) when (act) {
            is DataType.Nullable<*, *> ->
                throw AssertionError()
            is DataType.NotNull.Simple<*> ->
                if (act.kind in storedAsString) editor.putBoolean(key, false) else editor.putString(key, "null")
            is DataType.NotNull.Collect<*, *, *> ->
                editor.putBoolean(key, false)
            is DataType.NotNull.Partial<*, *> ->
                editor.putBoolean(key, false)
        }.also { return }

        act as DataType<T/*!!*/>
    } else this

    when (type) {
        is DataType.Nullable<*, *> -> throw AssertionError()
        is DataType.NotNull.Simple<T> ->
            if (type.hasStringRepresentation) editor.putString(key, type.storeAsString(value).toString())
            else type.store(value).let { v -> when (type.kind) {
                DataType.NotNull.Simple.Kind.Bool -> editor.putBoolean(key, v as Boolean)
                DataType.NotNull.Simple.Kind.I32 -> editor.putInt(key, v as Int)
                DataType.NotNull.Simple.Kind.I64 -> editor.putLong(key, v as Long)
                DataType.NotNull.Simple.Kind.F32 -> editor.putFloat(key, v as Float)
                DataType.NotNull.Simple.Kind.F64 -> editor.putLong(key, java.lang.Double.doubleToLongBits(v as Double))
                DataType.NotNull.Simple.Kind.Str -> editor.putString(key, v as String)
                DataType.NotNull.Simple.Kind.Blob -> editor.putString(key, Base64.encodeToString(v as ByteArray, Base64.DEFAULT))
                else -> throw AssertionError()
            } }
        is DataType.NotNull.Collect<T, *, *> -> type.elementType.let { elementType ->
            if (elementType is DataType.NotNull.Simple && (elementType.hasStringRepresentation || elementType.kind == DataType.NotNull.Simple.Kind.Str))
                editor.putStringSet(
                    key,
                    type.store(value)
                        .fatMapTo<HashSet<String>, T, String>(HashSet()) { v ->
                            (elementType as DataType.NotNull.Simple<T>)
                                .let { if (it.hasStringRepresentation) it.storeAsString(v) else it.store(v) as CharSequence }
                                .toString()
                        }
                )
            else
                editor.putString(key, Base64.encodeToString(serialized(type).store(value) as ByteArray, Base64.DEFAULT))
        }
        is DataType.NotNull.Partial<*, *> ->
            editor.putString(key, Base64.encodeToString(serialized(type).store(value) as ByteArray, Base64.DEFAULT))
    }
}
