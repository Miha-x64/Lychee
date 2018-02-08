package net.aquadc.properties.internal

import net.aquadc.properties.Property


class UnsynchronizedBiMappedCachedReferenceProperty<in A, in B, out T>(
        private val a: Property<A>,
        private val b: Property<B>,
        private val transform: (A, B) -> T
): Property<T> {

    init {
        if (a is ImmutableReferenceProperty)
            throw IllegalArgumentException("immutable property $a should not be mapped")
        if (b is ImmutableReferenceProperty)
            throw IllegalArgumentException("immutable property $b should not be mapped")
    }

    private val thread = Thread.currentThread()

    override val mayChange: Boolean get() {
        checkThread(thread)
        return true
    }

    override val isConcurrent: Boolean get() {
        checkThread(thread)
        return false
    }

    override val value: T
        get() {
            checkThread(thread)
            return valueRef
        }

    private var valueRef: T = transform(a.value, b.value)

    init {
        a.addChangeListener { _, new -> recalculate(new, b.value) }
        b.addChangeListener { _, new -> recalculate(a.value, new) }
    }

    private val listeners = ArrayList<(T, T) -> Unit>()

    private fun recalculate(newA: A, newB: B) {
        val new = transform(newA, newB)
        val old = value
        valueRef = new
        if (new !== old) {
            listeners.forEach { it(old, new) }
        }
    }

    override fun addChangeListener(onChange: (old: T, new: T) -> Unit) {
        checkThread(thread)
        listeners.add(onChange)
    }

    override fun removeChangeListener(onChange: (old: T, new: T) -> Unit) {
        checkThread(thread)
        listeners.remove(onChange)
    }

}
