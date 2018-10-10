package net.aquadc.persistence.type

import android.content.SharedPreferences


interface PrefsConverter<T> : Converter<T> {

    /**
     * Reads a value from [prefs].
     */
    fun get(prefs: SharedPreferences, key: String, default: T): T

    /**
     * Puts a value into [editor].
     * It's client's responsibility to begin a transaction and to commit it.
     */
    fun put(editor: SharedPreferences.Editor, key: String, value: T)

}
