package net.aquadc.properties.internal

import net.aquadc.properties.MutableProperty
import net.aquadc.properties.Property
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater


class ConcurrentMutableReferenceProperty<T>(
        value: T
) : MutableProperty<T> {

    @Volatile @Suppress("UNUSED")
    private var valueRef: T = value

    override var value: T
        get() {
            val sample = sampleUpdater<T>().get(this)
            return if (sample == null) valueUpdater<T>().get(this) else sample.value
        }
        set(new) {
            // if bound, unbind
            val oldSample = sampleUpdater<T>().getAndSet(this, null)
            oldSample?.removeChangeListener(onChangeInternal)

            // update then
            val old: T = valueUpdater<T>().getAndSet(this, new)

            onChangeInternal(old, new)
        }

    @Volatile @Suppress("UNUSED")
    private var sample: Property<T>? = null

    override val mayChange: Boolean get() = true
    override val isConcurrent: Boolean get() = true

    override fun bindTo(sample: Property<T>) {
        val newSample = if (sample.mayChange) sample else null
        val oldSample = sampleUpdater<T>().getAndSet(this, newSample)
        oldSample?.removeChangeListener(onChangeInternal)
        newSample?.addChangeListener(onChangeInternal)

        val new = sample.value
        val old = valueUpdater<T>().getAndSet(this, new)
        onChangeInternal(old, new)
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
        val ValueUpdater: AtomicReferenceFieldUpdater<ConcurrentMutableReferenceProperty<*>, Any?> =
                AtomicReferenceFieldUpdater.newUpdater(ConcurrentMutableReferenceProperty::class.java, Any::class.java, "valueRef")
        val SampleUpdater: AtomicReferenceFieldUpdater<ConcurrentMutableReferenceProperty<*>, Property<*>?> =
                AtomicReferenceFieldUpdater.newUpdater(ConcurrentMutableReferenceProperty::class.java, Property::class.java, "sample")

        inline fun <T> valueUpdater() =
                ValueUpdater as AtomicReferenceFieldUpdater<ConcurrentMutableReferenceProperty<T>, T>
        inline fun <T> sampleUpdater() =
                SampleUpdater as AtomicReferenceFieldUpdater<ConcurrentMutableReferenceProperty<T>, Property<T>>
    }

}
