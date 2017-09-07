package net.aquadc.properties.internal

import net.aquadc.properties.MutableProperty
import net.aquadc.properties.Property
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

class ConcurrentMutableReferenceProperty<T>(
        value: T
) : MutableProperty<T> {

    private val valueReference = AtomicReference<T>(value)
    override var value: T
        get() {
            val sample = sample.get()
            return if (sample == null) valueReference.get() else sample.value
        }
        set(new) {
            val old: T = valueReference.getAndSet(new)

            // if bound, unbind
            val oldSample = sample.getAndSet(null)
            oldSample?.removeChangeListener(onChangeInternal)

            onChangeInternal(old, new)
        }

    private val sample = AtomicReference<Property<T>?>(null)

    override val mayChange: Boolean get() = true
    override val isConcurrent: Boolean get() = true

    override fun bindTo(sample: Property<T>) {
        val newSample = if (sample.mayChange) sample else null
        val oldSample = this.sample.getAndSet(newSample)
        oldSample?.removeChangeListener(onChangeInternal)
        newSample?.addChangeListener(onChangeInternal)

        val new = sample.value
        val old = valueReference.getAndSet(new)
        onChangeInternal(old, new)
    }

    private val onChangeInternal: (T, T) -> Unit = this::onChangeInternal
    private fun onChangeInternal(old: T, new: T) {
        if (new !== old) {
            listeners.forEach { it(old, new) }
        }
    }

    private val listeners = CopyOnWriteArrayList<(T, T) -> Unit>()

    override fun addChangeListener(onChange: (old: T, new: T) -> Unit) {
        listeners.add(onChange)
    }

    override fun removeChangeListener(onChange: (old: T, new: T) -> Unit) {
        listeners.remove(onChange)
    }

}
