package net.aquadc.properties.internal

import net.aquadc.properties.ChangeListener
import net.aquadc.properties.Property
import net.aquadc.properties.addUnconfinedChangeListener
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater


internal class `BiMapped-`<in A, in B, out T>(
        private val a: Property<A>,
        private val b: Property<B>,
        private val transform: (A, B) -> T
) : PropNotifier<T>(threadIfNot(a.isConcurrent && b.isConcurrent)), ChangeListener<Any?> {

    @Volatile @Suppress("UNUSED")
    private var valueRef: T = unset()
    init {
        check(a.mayChange || b.mayChange)
    }

    override val value: T
        get() {
            if (thread !== null) checkThread()
            val value = valueUpdater<T>().get(this)

            // if not observed, calculate on demand
            return if (value === Unset) transform(a.value, b.value) else value
        }

    override fun invoke(_old: Any?, _new: Any?) {
        val new = transform(a.value, b.value)
        val old: T
        if (thread === null) {
            old = valueUpdater<T>().getAndSet(this, new)
        } else {
            old = valueRef
            valueUpdater<T>().lazySet(this, new)
        }
        valueChanged(old, new, null)
    }

    override fun observedStateChanged(observed: Boolean) {
        if (observed) {
            val mapped = transform(a.value, b.value)
            valueUpdater<T>().eagerOrLazySet(this, thread, mapped)
            if (a.mayChange) a.addUnconfinedChangeListener(this)
            if (b.mayChange) b.addUnconfinedChangeListener(this)
        } else {
            if (a.mayChange) a.removeChangeListener(this)
            if (b.mayChange) b.removeChangeListener(this)
            valueUpdater<T>().eagerOrLazySet(this, thread, unset())
        }
    }

    private companion object {
        @JvmField internal val ValueUpdater: AtomicReferenceFieldUpdater<`BiMapped-`<*, *, *>, Any?> =
                AtomicReferenceFieldUpdater.newUpdater(`BiMapped-`::class.java, Any::class.java, "valueRef")

        @Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
        private inline fun <T> valueUpdater() =
                ValueUpdater as AtomicReferenceFieldUpdater<`BiMapped-`<*, *, T>, T>
    }

}
