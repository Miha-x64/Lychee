package net.aquadc.properties.internal

import net.aquadc.properties.Property


class UnsBiMappedProperty<in A, in B, out T>(
        a: Property<A>,
        b: Property<B>,
        private val transform: (A, B) -> T
) : PropNotifier<T>(Thread.currentThread()) {

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

    @Suppress("MemberVisibilityCanBePrivate") // produce no access$
    internal fun recalculate(newA: A, newB: B) {
        val new = transform(newA, newB)
        val old = valueRef
        valueRef = new
        valueChanged(old, new, null)
    }

}
