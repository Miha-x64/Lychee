package net.aquadc.properties.internal

import net.aquadc.properties.MutableProperty
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

class ConcurrentMutableReferenceProperty<T>(
        value: T
) : MutableProperty<T> {

    private val valueReference = AtomicReference<T>(value)
    override var value: T
        get() = valueReference.get()
        set(new) {
            val old: T = valueReference.getAndSet(new)
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