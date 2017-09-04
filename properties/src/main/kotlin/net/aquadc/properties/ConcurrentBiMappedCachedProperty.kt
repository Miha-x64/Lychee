package net.aquadc.properties

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

class ConcurrentBiMappedCachedProperty<A, B, out T>(
        private val a: Property<A>,
        private val b: Property<B>,
        private val transform: (A, B) -> T
): Property<T> {

    private val valueReference = AtomicReference<T>(transform(a.value, b.value))
    init {
        a.addChangeListener { _, new -> recalculate(new, b.value) }
        b.addChangeListener { _, new -> recalculate(a.value, new) }
    }

    override val value: T get() = valueReference.get()

    private val listeners = CopyOnWriteArrayList<(T, T) -> Unit>()

    private fun recalculate(newA: A, newB: B) {
        var old: T
        var new: T
        do {
            old = valueReference.get()
            new = transform(newA, newB)
        } while (!valueReference.compareAndSet(old, new))

        listeners.forEach { it(old, new) }
    }

    override fun addChangeListener(onChange: (old: T, new: T) -> Unit) {
        listeners.add(onChange)
    }

    override fun removeChangeListener(onChange: (old: T, new: T) -> Unit) {
        listeners.remove(onChange)
    }

}