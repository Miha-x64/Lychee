package net.aquadc.properties.android.pref

import android.content.SharedPreferences

/**
 * Generic adapter to SharedPreferences#get* and SharedPreferences.Editor#set*.
 */
@Deprecated("use converters from :persistence instead")
interface PrefAdapter<T> {

    /**
     * Reads and returns a value from [prefs] by [key].
     * Returns [default] if there's no such mapping.
     */
    fun read(prefs: SharedPreferences, key: String, default: T): T

    /**
     * Maps [key] to [value] in [editor].
     * Must not call [SharedPreferences.Editor.apply] or [SharedPreferences.Editor.commit].
     */
    fun save(editor: SharedPreferences.Editor, key: String, value: T)

    /**
     * Checks whether [propKey] is for [prefKey].
     * If [read] and [save] functions transform passed keys somehow,
     * should implement some checking.
     *
     * For example, when a single [PrefAdapter] stands for several [SharedPreferences] values,
     * e. g. username and email, say, as `${propKey}_name` and `${propKey}_email`,
     * then it should return `true` for each property,
     * i. e. both `isKeyFor("someKey", "someKey_name" and "someKey_email")`,
     * to help properties keeping data up to date.
     */
    @Deprecated("This is quite strange. Will be removed")
    fun isKeyFor(propKey: String, prefKey: String): Boolean
}

/**
 * Base adapter for properties which map to single [SharedPreferences] mapping.
 */
abstract class SimplePrefAdapter<T> : PrefAdapter<T> {

    final override fun isKeyFor(propKey: String, prefKey: String): Boolean =
            propKey == prefKey

}

/**
 * Adapts [SharedPreferences.getString] and [SharedPreferences.Editor.putString].
 */
@Deprecated("use converters from :persistence instead", ReplaceWith("string", "net.aquadc.persistence.converter.string"))
object StringPrefAdapter : SimplePrefAdapter<String>() {

    override fun read(prefs: SharedPreferences, key: String, default: String): String =
            prefs.getString(key, default)

    override fun save(editor: SharedPreferences.Editor, key: String, value: String) {
        editor.putString(key, value)
    }

}

/**
 * Adapts [SharedPreferences.getStringSet] and [SharedPreferences.Editor.putStringSet].
 */
@Deprecated("use converters from :persistence instead") // TODO: converter for Set<String>?..
object StringSetPrefAdapter : SimplePrefAdapter<Set<String>>() {

    override fun read(prefs: SharedPreferences, key: String, default: Set<String>): Set<String> =
            prefs.getStringSet(key, default)

    override fun save(editor: SharedPreferences.Editor, key: String, value: Set<String>) {
        editor.putStringSet(key, value)
    }

}

/**
 * Adapts [SharedPreferences.getInt] and [SharedPreferences.Editor.putInt].
 */
@Deprecated("use converters from :persistence instead", ReplaceWith("int", "net.aquadc.persistence.converter.int"))
object IntPrefAdapter : SimplePrefAdapter<Int>() {

    override fun read(prefs: SharedPreferences, key: String, default: Int): Int =
            prefs.getInt(key, default)

    override fun save(editor: SharedPreferences.Editor, key: String, value: Int) {
        editor.putInt(key, value)
    }

}

/**
 * Adapts [SharedPreferences.getLong] and [SharedPreferences.Editor.putLong].
 */
@Deprecated("use converters from :persistence instead", ReplaceWith("long", "net.aquadc.persistence.converter.long"))
object LongPrefAdapter : SimplePrefAdapter<Long>() {

    override fun read(prefs: SharedPreferences, key: String, default: Long): Long =
            prefs.getLong(key, default)

    override fun save(editor: SharedPreferences.Editor, key: String, value: Long) {
        editor.putLong(key, value)
    }

}

/**
 * Adapts [SharedPreferences.getFloat] and [SharedPreferences.Editor.putFloat].
 */
@Deprecated("use converters from :persistence instead", ReplaceWith("float", "net.aquadc.persistence.converter.float"))
object FloatPrefAdapter : SimplePrefAdapter<Float>() {

    override fun read(prefs: SharedPreferences, key: String, default: Float): Float =
            prefs.getFloat(key, default)

    override fun save(editor: SharedPreferences.Editor, key: String, value: Float) {
        editor.putFloat(key, value)
    }

}

/**
 * Adapts [SharedPreferences.getLong] + [java.lang.Double.doubleToRawLongBits]
 * and [SharedPreferences.Editor.putLong] + [java.lang.Double.longBitsToDouble].
 */
@Deprecated("use converters from :persistence instead", ReplaceWith("double", "net.aquadc.persistence.converter.double"))
object DoublePrefAdapter : SimplePrefAdapter<Double>() {

    override fun read(prefs: SharedPreferences, key: String, default: Double): Double {
        val value = prefs.all[key] as Long?
        return value?.let(java.lang.Double::longBitsToDouble) ?: default
    }

    override fun save(editor: SharedPreferences.Editor, key: String, value: Double) {
        editor.putLong(key, java.lang.Double.doubleToRawLongBits(value))
    }

}

/**
 * Adapts [SharedPreferences.getBoolean] and [SharedPreferences.Editor.putBoolean].
 */
@Deprecated("use converters from :persistence instead", ReplaceWith("bool", "net.aquadc.persistence.converter.bool"))
object BoolPrefAdapter : SimplePrefAdapter<Boolean>() {

    override fun read(prefs: SharedPreferences, key: String, default: Boolean): Boolean =
            prefs.getBoolean(key, default)

    override fun save(editor: SharedPreferences.Editor, key: String, value: Boolean) {
        editor.putBoolean(key, value)
    }

}
