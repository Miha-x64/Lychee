package net.aquadc.properties.internal

import net.aquadc.properties.Property


class UnsBiMappedProperty<in A, in B, out T>(
        private val a: Property<A>,
        private val b: Property<B>,
        private val transform: (A, B) -> T
) : UnsListeners<T>() {

    init {
        check(a !is ImmutableReferenceProperty)
        check(b !is ImmutableReferenceProperty)

        a.addChangeListener { _, new -> recalculate(new, b.getValue()) }
        b.addChangeListener { _, new -> recalculate(a.getValue(), new) }
    }

    override fun getValue(): T {
        checkThread()
        return valueRef
    }

    private var valueRef: T = transform(a.getValue(), b.getValue())

    private fun recalculate(newA: A, newB: B) {
        val new = transform(newA, newB)
        val old = valueRef
        valueRef = new
        listeners.notifyAll(old, new)
    }

}
