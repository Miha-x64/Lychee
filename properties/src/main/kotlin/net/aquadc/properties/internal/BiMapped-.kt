package net.aquadc.properties.internal

import net.aquadc.properties.ChangeListener
import net.aquadc.properties.Property
import net.aquadc.properties.addUnconfinedChangeListener


internal class `BiMapped-`<in A, in B, out T>(
        private val a: Property<A>,
        private val b: Property<B>,
        private val transform: (A, B) -> T
) : `Notifier+1AtomicRef`<T, @UnsafeVariance T>(
        a.isConcurrent && b.isConcurrent, unset()
), ChangeListener<Any?> {

    init {
        check(a.mayChange || b.mayChange)
    }

    override val value: T
        get() {
            if (thread !== null) checkThread()
            val value = ref

            // if not observed, calculate on demand
            return if (value === Unset) transform(a.value, b.value) else value
        }

    override fun invoke(_old: Any?, _new: Any?) {
        val new = transform(a.value, b.value)
        val old: T
        if (thread === null) {
            old = refUpdater().getAndSet(this, new)
        } else {
            old = ref
            refUpdater().lazySet(this, new)
        }
        valueChanged(old, new, null)
    }

    override fun observedStateChanged(observed: Boolean) {
        if (observed) {
            val mapped = transform(a.value, b.value)
            refUpdater().eagerOrLazySet(this, thread, mapped)
            if (a.mayChange) a.addUnconfinedChangeListener(this)
            if (b.mayChange) b.addUnconfinedChangeListener(this)
        } else {
            if (a.mayChange) a.removeChangeListener(this)
            if (b.mayChange) b.removeChangeListener(this)
            refUpdater().eagerOrLazySet(this, thread, unset())
        }
    }

}
