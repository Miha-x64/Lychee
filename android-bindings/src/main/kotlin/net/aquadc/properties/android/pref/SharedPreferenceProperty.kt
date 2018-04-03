package net.aquadc.properties.android.pref

import android.content.SharedPreferences
import net.aquadc.properties.MutableProperty
import net.aquadc.properties.Property
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater

/**
 * Wraps a value from [SharedPreferences].
 * Caveats:
 * * [SharedPreferences.OnSharedPreferenceChangeListener.onSharedPreferenceChanged] is being called on main thread
 * * when bound, there will be some lag between source value change and change notification
 * * CAS is not a straight CAS, may be inaccurate a bit
 */
class SharedPreferenceProperty<T>(
        private val prefs: SharedPreferences,
        private val key: String,
        private val defaultValue: T,
        private val adapter: PrefAdapter<T>
) : MutableProperty<T> {

    // we need a strong reference because shared prefs holding a weak one
    private val prefChangeListener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, key -> changed(key) }

    init {
        prefs.registerOnSharedPreferenceChangeListener(prefChangeListener)
    }

    @Suppress("MemberVisibilityCanBePrivate") // internal — to avoid synthetic accessors
    internal fun changed(key: String) {
        if (adapter.isKeyFor(this.key, key)) {
            val new = adapter.read(prefs, this.key, defaultValue)
            val old = valueUpdater<T>().getAndSet(this, new)
            listeners.forEach { it(old, new) }
        }
    }

    @Volatile @Suppress("UNUSED")
    private var valueRef: T = adapter.read(prefs, key, defaultValue)

    override var value: T
        get() = valueUpdater<T>().get(this)
        set(newValue) {
            dropBinding()

            // update then
            val ed = prefs.edit()
            adapter.save(ed, key, newValue)
            ed.apply()
        }

    @Volatile @Suppress("UNUSED")
    private var sample: Property<T>? = null

    override val mayChange: Boolean get() = true
    override val isConcurrent: Boolean get() = true

    override fun bindTo(sample: Property<T>) {
        val newSample = if (sample.mayChange) sample else null
        val oldSample = sampleUpdater<T>().getAndSet(this, newSample)
        oldSample?.removeChangeListener(sampleChanged)
        newSample?.addChangeListener(sampleChanged)

        val ed = prefs.edit()
        adapter.save(ed, key, sample.value)
        ed.apply()
    }

    // may be inaccurate
    override fun casValue(expect: T, update: T): Boolean {
        dropBinding()
        return if (valueRef === expect) {
            value = update
            true
        } else {
            false
        }
    }

    private fun dropBinding() {
        val oldSample = sampleUpdater<T>().getAndSet(this, null)
        oldSample?.removeChangeListener(sampleChanged)
    }

    private val sampleChanged: (T, T) -> Unit = { _, new -> sampleChanged(new) }

    @Suppress("MemberVisibilityCanBePrivate") // using internal to avoid synthetic accessors
    internal fun sampleChanged(new: T) {
        val ed = prefs.edit()
        adapter.save(ed, key, new)
        ed.apply()
    }

    private val listeners = CopyOnWriteArrayList<(T, T) -> Unit>()

    override fun addChangeListener(onChange: (old: T, new: T) -> Unit) {
        listeners.add(onChange)
    }

    override fun removeChangeListener(onChange: (old: T, new: T) -> Unit) {
        listeners.remove(onChange)
    }

    @Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST") // just safe unchecked cast, should produce no bytecode
    private companion object {
        @JvmField
        val ValueUpdater: AtomicReferenceFieldUpdater<SharedPreferenceProperty<*>, Any?> =
                AtomicReferenceFieldUpdater.newUpdater(SharedPreferenceProperty::class.java, Any::class.java, "valueRef")
        @JvmField
        val SampleUpdater: AtomicReferenceFieldUpdater<SharedPreferenceProperty<*>, Property<*>?> =
                AtomicReferenceFieldUpdater.newUpdater(SharedPreferenceProperty::class.java, Property::class.java, "sample")

        inline fun <T> valueUpdater() =
                ValueUpdater as AtomicReferenceFieldUpdater<SharedPreferenceProperty<T>, T>
        inline fun <T> sampleUpdater() =
                SampleUpdater as AtomicReferenceFieldUpdater<SharedPreferenceProperty<T>, Property<T>>
    }

}
