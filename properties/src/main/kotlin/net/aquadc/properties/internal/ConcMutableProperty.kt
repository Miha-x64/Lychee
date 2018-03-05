package net.aquadc.properties.internal

import net.aquadc.properties.MutableProperty
import net.aquadc.properties.Property
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater

/**
 * Concurrent [MutableProperty] implementation.
 */
class ConcMutableProperty<T>(
        value: T
) : BaseConcProperty<T>(), MutableProperty<T> {

    @Volatile @Suppress("UNUSED")
    private var valueRef: T = value

    override fun getValue(): T {
        val sample = sampleUpdater<T>().get(this)
        return if (sample == null) valueUpdater<T>().get(this) else sample.getValue()
    }

    override fun setValue(newValue: T) {
        dropBinding()
        val old: T = valueUpdater<T>().getAndSet(this, newValue)
        onChangeInternal(old, newValue)
    }

    @Volatile @Suppress("UNUSED")
    private var sample: Property<T>? = null

    override fun bindTo(sample: Property<T>) {
        val newSample = if (sample.mayChange) sample else null
        val oldSample = sampleUpdater<T>().getAndSet(this, newSample)
        oldSample?.removeChangeListener(onChangeInternal)
        newSample?.addChangeListener(onChangeInternal)

        val new = sample.getValue()
        val old = valueUpdater<T>().getAndSet(this, new)
        onChangeInternal(old, new)
    }

    override fun cas(expect: T, update: T): Boolean {
        dropBinding()
        return if (valueUpdater<T>().compareAndSet(this, expect, update)) {
            onChangeInternal(expect, update)
            true
        } else {
            false
        }
    }

    private fun dropBinding() {
        val oldSample = sampleUpdater<T>().getAndSet(this, null)
        oldSample?.removeChangeListener(onChangeInternal)
    }

    private val onChangeInternal: (T, T) -> Unit = this::onChangeInternal
    private fun onChangeInternal(old: T, new: T) {
        listeners.forEach { it(old, new) }
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
        val ValueUpdater: AtomicReferenceFieldUpdater<ConcMutableProperty<*>, Any?> =
                AtomicReferenceFieldUpdater.newUpdater(ConcMutableProperty::class.java, Any::class.java, "valueRef")
        val SampleUpdater: AtomicReferenceFieldUpdater<ConcMutableProperty<*>, Property<*>?> =
                AtomicReferenceFieldUpdater.newUpdater(ConcMutableProperty::class.java, Property::class.java, "sample")

        inline fun <T> valueUpdater() =
                ValueUpdater as AtomicReferenceFieldUpdater<ConcMutableProperty<T>, T>
        inline fun <T> sampleUpdater() =
                SampleUpdater as AtomicReferenceFieldUpdater<ConcMutableProperty<T>, Property<T>>
    }

}
