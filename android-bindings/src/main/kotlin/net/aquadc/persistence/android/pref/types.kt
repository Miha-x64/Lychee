package net.aquadc.persistence.android.pref

import android.content.SharedPreferences
import android.util.Base64
import net.aquadc.collections.contains
import net.aquadc.collections.plus
import net.aquadc.persistence.fatMapTo
import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.DataTypeVisitor
import net.aquadc.persistence.type.match
import net.aquadc.persistence.type.serialized
import net.aquadc.properties.internal.Unset
import java.lang.Double as JavaLangDouble


internal fun <SCH : Schema<SCH>, T> SCH.get(what: FieldDef<SCH, T, *>, prefs: SharedPreferences): T {
    val value = typeOf(what as FieldDef<SCH, T, DataType<T>>).get(prefs, nameOf(what).toString())
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
@JvmSynthetic internal val storedAsString = DataType.Simple.Kind.Str + DataType.Simple.Kind.Blob

@Suppress("UNCHECKED_CAST")
private fun <T> DataType<T>.get(prefs: SharedPreferences, key: String): T {
    if (!prefs.contains(key)) return Unset as T

    val map = prefs.all // sadly, copying prefs fully is the only way to achieve correctness concurrently

    val value = map[key]
    if (value === null) return Unset as T

    return (readerVis as PrefReaderVisitor<T>).match(this, null, value)
}
private val readerVis = PrefReaderVisitor<Any?>()
private class PrefReaderVisitor<T> : DataTypeVisitor<Nothing?, Any, T, T> {

    override fun Nothing?.simple(arg: Any, nullable: Boolean, type: DataType.Simple<T>): T =
            if (nullable && if (type.kind in storedAsString) arg == false else arg == "null")
                null as T
            else
                type.load(when (type.kind) {
                    DataType.Simple.Kind.Bool -> arg as Boolean
                    DataType.Simple.Kind.I32 -> arg as Int
                    DataType.Simple.Kind.I64 -> arg as Long
                    DataType.Simple.Kind.F32 -> arg as Float
                    DataType.Simple.Kind.F64 -> JavaLangDouble.longBitsToDouble(arg as Long)
                    DataType.Simple.Kind.Str -> arg as String
                    DataType.Simple.Kind.Blob -> Base64.decode(arg as String, Base64.DEFAULT)
                    else -> throw AssertionError()
                })

    override fun <E> Nothing?.collection(arg: Any, nullable: Boolean, type: DataType.Collect<T, E, out DataType<E>>): T =
            if (nullable && arg == false)
                null as T
            else
                type.elementType.let { elementType ->
                    if (elementType is DataType.Simple<*> && elementType.kind == DataType.Simple.Kind.Str) // TODO should store everything in strings
                        type.load((arg as Set<String>).map(elementType::load)) // todo zero-copy
                    else /* here we have a Collection<Whatever>, including potentially a collection of collections, structs, etc */
                        serialized(type).load(Base64.decode(arg as String, Base64.DEFAULT))
                }

    override fun <SCH : Schema<SCH>> Nothing?.partial(arg: Any, nullable: Boolean, type: DataType.Partial<T, SCH>): T =
            if (nullable && arg == false) null as T
            else serialized(type).load(Base64.decode(arg as String, Base64.DEFAULT))

}

internal fun <T> DataType<T>.put(editor: SharedPreferences.Editor, key: String, value: T) {
    PrefWriterVisitor<T>(key).match(this, editor, value)
}
private class PrefWriterVisitor<T>(
        private val key: String
) : DataTypeVisitor<SharedPreferences.Editor, T, T, Unit> {

    override fun SharedPreferences.Editor.simple(arg: T, nullable: Boolean, type: DataType.Simple<T>) =
            if (nullable && arg === null) {
                if (type.kind in storedAsString) putBoolean(key, false)
                else putString(key, "null")
            } else {
                val v = type.store(arg)
                when (type.kind) {
                    DataType.Simple.Kind.Bool -> putBoolean(key, v as Boolean)
                    DataType.Simple.Kind.I32 -> putInt(key, v as Int)
                    DataType.Simple.Kind.I64 -> putLong(key, v as Long)
                    DataType.Simple.Kind.F32 -> putFloat(key, v as Float)
                    DataType.Simple.Kind.F64 -> putLong(key, java.lang.Double.doubleToLongBits(v as Double))
                    DataType.Simple.Kind.Str -> putString(key, v as String)
                    DataType.Simple.Kind.Blob -> putString(key, Base64.encodeToString(v as ByteArray, Base64.DEFAULT))
                    else -> throw AssertionError()
                }
            }.let { }

    override fun <E> SharedPreferences.Editor.collection(arg: T, nullable: Boolean, type: DataType.Collect<T, E, out DataType<E>>) =
            if (nullable && arg === null) {
                putBoolean(key, false)
            } else {
                type.elementType.let { elementType ->
                    if (elementType is DataType.Simple<E> && elementType.kind == DataType.Simple.Kind.Str)
                        putStringSet(key, type.store(arg).fatMapTo<HashSet<String>, E, String>(HashSet()) { elementType.store(it) as String })
                    else
                        putString(key, Base64.encodeToString(serialized(type).store(arg) as ByteArray, Base64.DEFAULT))
                }
            }.let { }

    override fun <SCH : Schema<SCH>> SharedPreferences.Editor.partial(arg: T, nullable: Boolean, type: DataType.Partial<T, SCH>) {
        if (nullable && arg === null) putBoolean(key, false)
        else putString(key, Base64.encodeToString(serialized(type).store(arg) as ByteArray, Base64.DEFAULT))
    }

}
