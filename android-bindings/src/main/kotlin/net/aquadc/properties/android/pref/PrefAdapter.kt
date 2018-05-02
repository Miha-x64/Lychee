package net.aquadc.properties.android.pref

import android.content.SharedPreferences

/**
 * Generic adapter to SharedPreferences#get* and SharedPreferences.Editor#set*.
 */
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
    fun isKeyFor(propKey: String, prefKey: String): Boolean
}

/**
 * Adapts [SharedPreferences.getString] and [SharedPreferences.Editor.putString].
 */
object StringPrefAdapter : PrefAdapter<String> {

    override fun read(prefs: SharedPreferences, key: String, default: String): String =
            prefs.getString(key, default)

    override fun save(editor: SharedPreferences.Editor, key: String, value: String) {
        editor.putString(key, value)
    }

    override fun isKeyFor(propKey: String, prefKey: String): Boolean =
            propKey == prefKey

}
