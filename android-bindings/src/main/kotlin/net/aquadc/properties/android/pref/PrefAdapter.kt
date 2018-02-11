package net.aquadc.properties.android.pref

import android.content.SharedPreferences


interface PrefAdapter<T> {
    fun read(prefs: SharedPreferences, key: String, default: T): T
    fun save(editor: SharedPreferences.Editor, key: String, value: T)
    // Interesting one.
    // When you're holding several values (e. g. username and email), say, in ${propKey}_name and ${propKey}_email props
    // and writing them in a single transaction, you should return true for each property,
    // e. g. for `isKeyFor("someKey", "someKey_name" or "someKey_email")` to help property keeping data up to date.
    fun isKeyFor(propKey: String, prefKey: String): Boolean
}

object StringPrefAdapter : PrefAdapter<String> {

    override fun read(prefs: SharedPreferences, key: String, default: String): String =
            prefs.getString(key, default)

    override fun save(editor: SharedPreferences.Editor, key: String, value: String) {
        editor.putString(key, value)
    }

    override fun isKeyFor(propKey: String, prefKey: String): Boolean =
            propKey == prefKey

}
