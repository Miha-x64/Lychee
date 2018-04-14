package net.aquadc.properties.internal

import net.aquadc.properties.MutableProperty
import net.aquadc.properties.Property
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater

/**
 * Concurrent [MutableProperty] implementation.
 */
class ConcMutableProperty<T>(
        value: T
) : PropNotifier<T>(null), MutableProperty<T> {

    @Volatile @Suppress("UNUSED")
    private var valueRef: T = value

    override var value: T
        get() {
            val sample = sampleUpdater<T>().get(this)
            return if (sample == null) valueUpdater<T>().get(this) else sample.value
        }
        set(newValue) {
            dropBinding()
            val old: T = valueUpdater<T>().getAndSet(this, newValue)
            valueChanged(old, newValue, null)
        }

    @Volatile @Suppress("UNUSED")
    private var sample: Property<T>? = null

    override fun bindTo(sample: Property<T>) {
        val newSample = if (sample.mayChange) sample else null
        val oldSample = sampleUpdater<T>().getAndSet(this, newSample)
        oldSample?.removeChangeListener(onChangeInternal)
        newSample?.addChangeListener(onChangeInternal)

        val new = sample.value
        val old = valueUpdater<T>().getAndSet(this, new)
        valueChanged(old, new, null)
    }

    override fun casValue(expect: T, update: T): Boolean {
        dropBinding()
        return if (valueUpdater<T>().compareAndSet(this, expect, update)) {
            valueChanged(expect, update, null)
            true
        } else {
            false
        }
    }

    private fun dropBinding() {
        val oldSample = sampleUpdater<T>().getAndSet(this, null)
        oldSample?.removeChangeListener(onChangeInternal)
    }

    private var _onChangeInternal: ((T, T) -> Unit)? = null

    private val onChangeInternal: (T, T) -> Unit // non-thread-safe lazy, which is totally OK in this case
        get() = _onChangeInternal ?: { old: T, new: T -> valueChanged(old, new, null) }.also { _onChangeInternal = it }

    @Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST") // just safe unchecked cast, should produce no bytecode
    private companion object {
        @JvmField
        val ValueUpdater: AtomicReferenceFieldUpdater<ConcMutableProperty<*>, Any?> =
                AtomicReferenceFieldUpdater.newUpdater(ConcMutableProperty::class.java, Any::class.java, "valueRef")
        @JvmField
        val SampleUpdater: AtomicReferenceFieldUpdater<ConcMutableProperty<*>, Property<*>?> =
                AtomicReferenceFieldUpdater.newUpdater(ConcMutableProperty::class.java, Property::class.java, "sample")

        inline fun <T> valueUpdater() =
                ValueUpdater as AtomicReferenceFieldUpdater<ConcMutableProperty<T>, T>
        inline fun <T> sampleUpdater() =
                SampleUpdater as AtomicReferenceFieldUpdater<ConcMutableProperty<T>, Property<T>>
    }

}
