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
            return if (sample == null) valueReference.get() else sample.first.value
        }
        set(new) {
            val old: T = valueReference.getAndSet(new)

            // if bound, unbind
            val oldSample = sample.getAndSet(null)
            oldSample?.first?.removeChangeListener(oldSample.second)

            onChangeInternal(old, new)
        }

    private val sample = AtomicReference<Pair<Property<T>, (T, T) -> Unit>?>(null)

    override val mayChange: Boolean get() = true

    override fun bind(sample: Property<T>) {
        if (sample.mayChange) {
            // attempt to reuse previous listener
            val listener = this.sample.get()?.second ?: this::onChangeInternal
            this.sample.set(Pair(sample, listener))
            sample.addChangeListener(listener)
        } else {
            value = sample.value
        }
    }

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
