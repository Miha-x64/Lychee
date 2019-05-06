package net.aquadc.properties.android.persistence.pref

import android.content.SharedPreferences
import net.aquadc.persistence.type.DataType
import net.aquadc.properties.TransactionalProperty
import net.aquadc.properties.internal.`Notifier-1AtomicRef`

/**
 * Wraps a value from [SharedPreferences].
 * Note that [SharedPreferences.OnSharedPreferenceChangeListener.onSharedPreferenceChanged] is called on main thread
 */
class SharedPreferenceProperty<T>(
        private val prefs: SharedPreferences,
        private val key: String,
        private val defaultValue: T,
        private val type: DataType<T>
) : `Notifier-1AtomicRef`<T, T>(
        concurrent = true,
        initialRef = type.get(prefs, key, defaultValue)
), TransactionalProperty<SharedPreferences.Editor, T> {

    // we need a strong reference because shared prefs holding a weak one
    private val changeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key -> changed(key) }
    init {
        prefs.registerOnSharedPreferenceChangeListener(changeListener)
    }

    @Suppress("MemberVisibilityCanBePrivate") // internal — to avoid synthetic accessors
    internal fun changed(key: String) {
        if (this.key == key) {
            val new = type.get(prefs, this.key, defaultValue)
            val old = refUpdater().getAndSet(this, new)
            valueChanged(old, new, null)
        }
    }

    override var value: T
        get() = ref
        set(newValue) {
            val ed = prefs.edit()
            type.put(ed, key, newValue)
            ed.apply() // and wait until onSharedPreferenceChanged comes
        }

    override fun setValue(transaction: SharedPreferences.Editor, value: T) {
        type.put(transaction, key, value) // and pray that this one is ours...
    }

}
