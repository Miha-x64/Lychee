package net.aquadc.properties.internal

import net.aquadc.properties.ChangeListener
import net.aquadc.properties.Property
import net.aquadc.properties.addUnconfinedChangeListener


internal class `BiMapped-`<in A, in B, out T>(
        private val a: Property<A>,
        private val b: Property<B>,
        private val transform: (A, B) -> T
) : `Notifier-1AtomicRef`<T, Any?>(
        a.isConcurrent && b.isConcurrent, unset()
) {

    init {
        check(a.mayChange || b.mayChange)
    }

    override val value: T
        get() {
            if (thread !== null) checkThread()
            val value = ref

            // if not observed, calculate on demand
            return if (value === Unset) transform(a.value, b.value) else (value as Array<*>)[2] as T
        }

    private val aListener: ChangeListener<A> = listener(0)
    private val bListener: ChangeListener<B> = listener(1)
    private fun listener(idx: Int): ChangeListener<Any?> = { _, new ->
        update(idx, new)
    }

    @JvmSynthetic internal fun update(idx: Int, value: Any?) {
        val prev = ref as Array<Any?>
        val new = prev.clone()
        new[idx] = value
        new[2] = transform(new[0] as A, new[1] as B)
        val old: T =
                if (thread === null) {
                    refUpdater().getAndSet(this, new) as Array<Any?>
                } else {
                    refUpdater().lazySet(this, new)
                    prev
                }[2] as T
        valueChanged(old, new[2] as T, null)
    }

    override fun observedStateChanged(observed: Boolean) {
        if (observed) {
            val aVal = a.value
            val bVal = b.value
            val mapped = transform(aVal, bVal)
            refUpdater().eagerOrLazySet(this, thread, arrayOf(aVal, bVal, mapped))
            a.addUnconfinedChangeListener(aListener)
            b.addUnconfinedChangeListener(bListener)
        } else {
            a.removeChangeListener(aListener)
            b.removeChangeListener(bListener)
            refUpdater().eagerOrLazySet(this, thread, unset())
        }
    }

}
