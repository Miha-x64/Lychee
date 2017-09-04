package net.aquadc.properties

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

class ConcurrentMutableReferenceProperty<T>(
        value: T
) : MutableProperty<T> {

    private val valueReference = AtomicReference<T>(value)
    override var value: T
        get() = valueReference.get()
        set(new) {
            var old: T
            do {
                old = valueReference.get()
            } while (!valueReference.compareAndSet(old, new))

            listeners.forEach { it(old, new) }
        }

    private val listeners = CopyOnWriteArrayList<(T, T) -> Unit>()

    override fun addChangeListener(onChange: (old: T, new: T) -> Unit) {
        listeners.add(onChange)
    }

    override fun removeChangeListener(onChange: (old: T, new: T) -> Unit) {
        listeners.remove(onChange)
    }

}