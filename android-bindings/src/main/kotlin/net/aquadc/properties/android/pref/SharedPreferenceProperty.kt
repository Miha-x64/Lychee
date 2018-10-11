package net.aquadc.properties.android.pref

import android.content.SharedPreferences
import android.util.Base64
import net.aquadc.persistence.type.DataType
import net.aquadc.properties.ChangeListener
import net.aquadc.properties.MutableProperty
import net.aquadc.properties.Property
import net.aquadc.properties.internal.`-Notifier`
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater

/**
 * Wraps a value from [SharedPreferences].
 * Caveats:
 * * [SharedPreferences.OnSharedPreferenceChangeListener.onSharedPreferenceChanged] is being called on main thread
 * * when bound, there will be some lag between source value change and change notification
 * * CAS is not a straight CAS, may be inaccurate a bit
 */
class SharedPreferenceProperty<T>
@Deprecated("Use another constructor. This will be removed.") constructor(
        private val prefs: SharedPreferences,
        private val key: String,
        private val defaultValue: T,
        private val adapter: PrefAdapter<T>
) : `-Notifier`<T>(true), MutableProperty<T> {

    constructor(
            prefs: SharedPreferences,
            key: String,
            defaultValue: T,
            type: DataType<T>
    ) : this(
            prefs, key, defaultValue,
            object : SimplePrefAdapter<T>() {

                override fun read(prefs: SharedPreferences, key: String, default: T): T =
                        type.get(prefs, key, default)

                override fun save(editor: SharedPreferences.Editor, key: String, value: T) =
                        type.put(editor, key, value)

            }
    )

    // we need a strong reference because shared prefs holding a weak one
    private val changeListener = object :
            SharedPreferences.OnSharedPreferenceChangeListener, ChangeListener<T> {

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String) {
            changed(key)
        }

        // sample change listener
        override fun invoke(old: T, new: T) {
            sampleChanged(new)
        }

    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(changeListener)
    }

    @Suppress("MemberVisibilityCanBePrivate") // internal — to avoid synthetic accessors
    internal fun changed(key: String) {
        if (adapter.isKeyFor(this.key, key)) {
            val new = adapter.read(prefs, this.key, defaultValue)
            val old = valueUpdater<T>().getAndSet(this, new)
            valueChanged(old, new, null)
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

    @Synchronized
    override fun bindTo(sample: Property<T>) {
        val newSample = if (sample.mayChange) sample else null
        val oldSample = sampleUpdater<T>().getAndSet(this, newSample)
        oldSample?.removeChangeListener(changeListener)
        newSample?.addChangeListener(changeListener)

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
        oldSample?.removeChangeListener(changeListener)
    }

    @Suppress("MemberVisibilityCanBePrivate") // using internal to avoid synthetic accessors
    internal fun sampleChanged(new: T) {
        val ed = prefs.edit()
        adapter.save(ed, key, new)
        ed.apply()
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

// NOTE: `null` means 'absent' here
@Suppress("UNCHECKED_CAST")
private fun <T> DataType<T>.get(prefs: SharedPreferences, key: String, default: T): T {
    when (this) {
        is DataType.Str ->
            return prefs.getString(key, null)?.let(::asT) ?: default
        is DataType.Blob ->
            return prefs.getString(key, null)?.let { asT(Base64.decode(it, Base64.DEFAULT)) } ?: default
    }

    return if (isNullable) {
        when (this) {
            is DataType.Integer -> {
                val asNum = default?.let(::asNumber)
                when (sizeBits) {
                    1 -> prefs.getInt(key, (asNum as Boolean?).asInt(this)).asBoolean()?.let(::asT) as T
                    8 -> prefs.getInt(key, (asNum as Byte?)?.toInt() ?: Int.MIN_VALUE)
                            .let { if (it == Int.MIN_VALUE) null else asT(it.assertFitsByte()) } as T
                    16 -> prefs.getInt(key, (asNum as Short?)?.toInt() ?: Int.MIN_VALUE)
                            .let { if (it == Int.MIN_VALUE) null else asT(it.assertFitsShort()) } as T
                    32 -> prefs.getLong(key, (asNum as Int?)?.toLong() ?: Long.MIN_VALUE)
                            .let { if (it == Long.MIN_VALUE) null else asT(it.assertFitsInt()) } as T
                    64 -> prefs.getString(key, (asNum as Long?).toString())
                            .let { if (it == "null") null else asT(it.toLong()) } as T
                    else -> throw AssertionError()
                }
            }
            is DataType.Floating -> {
                val asNum = default?.let(::asNumber)
                when (sizeBits) {
                    32 -> prefs.getLong(key, (asNum as Float?)?.let(java.lang.Float::floatToIntBits)?.toLong() ?: Long.MIN_VALUE)
                            .let { if (it == Long.MIN_VALUE) null else asT(it.assertFitsInt().let(java.lang.Float::intBitsToFloat)) } as T
                    64 -> prefs.getString(key, (asNum as Double?).toString())
                            .let { if (it == "null") null else asT(it.toDouble()) } as T
                    else -> throw AssertionError()
                }
            }
            is DataType.Str, is DataType.Blob -> throw AssertionError()
        }
    } else {
        when (this) {
            is DataType.Integer -> {
                val defNum = asNumber(default)
                asT(when (sizeBits) {
                    1 -> prefs.getBoolean(key, defNum as Boolean)
                    8 -> prefs.getInt(key, (defNum as Byte).toInt()).assertFitsByte()
                    16 -> prefs.getInt(key, (defNum as Short).toInt()).assertFitsShort()
                    32 -> prefs.getInt(key, defNum as Int)
                    64 -> prefs.getLong(key, defNum as Long)
                    else -> throw AssertionError()
                })
            }
            is DataType.Floating -> {
                val defNum = asNumber(default)
                asT(when (sizeBits) {
                    32 -> prefs.getFloat(key, defNum as Float)
                    64 -> java.lang.Double.longBitsToDouble(prefs.getLong(key, java.lang.Double.doubleToLongBits(defNum as Double)))
                    else -> throw AssertionError()
                })
            }
            is DataType.Str, is DataType.Blob -> throw AssertionError()
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

private fun Long.assertFitsInt(): Int {
    if (this !in Int.MIN_VALUE..Int.MAX_VALUE)
        throw IllegalStateException("value ${this} cannot be fit into an int")
    return toInt()
}

private fun <T> Boolean?.asInt(type: DataType<T>): Int = when (this) {
    null -> {
        check(type.isNullable); -1
    }
    false -> 0
    true -> 1
}

private fun Int.asBoolean(): Boolean? {
    return when (this) {
        -1 -> null
        0 -> false
        1 -> true
        else -> throw AssertionError()
    }
}

private fun <T> DataType<T>.put(editor: SharedPreferences.Editor, key: String, value: T) {
    when (this) {
        is DataType.Str -> return editor.putString(key, value?.let(::asString)).ignored
        is DataType.Blob -> return editor.putString(key, value?.let { Base64.encodeToString(asByteArray(it), Base64.DEFAULT) }).ignored
    }

    return if (isNullable) {
        when (this) {
            is DataType.Integer -> {
                val asNum = value?.let(::asNumber)
                when (sizeBits) {
                    1 -> editor.putInt(key, (asNum as Boolean?).asInt(this)).ignored
                    8 -> editor.putInt(key, (asNum as Byte?)?.toInt() ?: Int.MIN_VALUE).ignored
                    16 -> editor.putInt(key, (asNum as Short?)?.toInt() ?: Int.MIN_VALUE).ignored
                    32 -> editor.putLong(key, (asNum as Int?)?.toLong() ?: Long.MIN_VALUE).ignored
                    64 -> editor.putString(key, (asNum as Long?).toString()).ignored
                    else -> throw AssertionError()
                }
            }
            is DataType.Floating -> {
                val asNum = value?.let(::asNumber)
                when (sizeBits) {
                    32 -> editor.putLong(key, (asNum as Float?)?.let(java.lang.Float::floatToIntBits)?.toLong() ?: Long.MIN_VALUE).ignored
                    64 -> editor.putString(key, (asNum as Double?).toString()).ignored
                    else -> throw AssertionError()
                }
            }
            is DataType.Str, is DataType.Blob -> throw AssertionError()
        }
    } else {
        when (this) {
            is DataType.Integer -> {
                val asNum = asNumber(value)
                when (sizeBits) {
                    1 -> editor.putBoolean(key, asNum as Boolean).ignored
                    8 -> editor.putInt(key, (asNum as Byte).toInt()).ignored
                    16 -> editor.putInt(key, (asNum as Short).toInt()).ignored
                    32 -> editor.putInt(key, asNum as Int).ignored
                    64 -> editor.putLong(key, asNum as Long).ignored
                    else -> throw AssertionError()
                }
            }
            is DataType.Floating -> {
                val asNum = asNumber(value)
                when (sizeBits) {
                    32 -> editor.putFloat(key, asNum as Float).ignored
                    64 -> editor.putLong(key, java.lang.Double.doubleToLongBits(asNum as Double)).ignored
                    else -> throw AssertionError()
                }
            }
            is DataType.Str, is DataType.Blob -> throw AssertionError()
        }
    }

}

@Suppress("unused")
private inline val Any?.ignored get() = Unit
