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

        a.addChangeListener { _, new -> recalculate(new, b.value) }
        b.addChangeListener { _, new -> recalculate(a.value, new) }
    }

    override val value: T
        get() {
            checkThread()
            return valueRef
        }

    private var valueRef: T = transform(a.value, b.value)

    private fun recalculate(newA: A, newB: B) {
        val new = transform(newA, newB)
        val old = value
        valueRef = new
        listeners.notifyAll(old, new)
    }

}
