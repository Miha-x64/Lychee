package net.aquadc.properties.internal

import net.aquadc.properties.ChangeListener
import net.aquadc.properties.Property
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater


internal class BiMappedProperty<in A, in B, out T>(
        private val a: Property<A>,
        private val b: Property<B>,
        private val transform: (A, B) -> T
) : PropNotifier<T>(threadIfNot(a.isConcurrent && b.isConcurrent)), ChangeListener<Any?> {

    @Volatile @Suppress("UNUSED")
    private var valueRef = this as T // this means 'not observed'
    init {
        check(a.mayChange || b.mayChange)
    }

    override val value: T
        get() {
            if (thread !== null) checkThread()
            val value = valueUpdater<T>().get(this)

            // if not observed, calculate on demand
            return if (value === this) transform(a.value, b.value) else value
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

    override fun observedStateChangedWLocked(observed: Boolean) {
        if (observed) {
            val mapped = transform(a.value, b.value)
            valueUpdater<T>().eagerOrLazySet(this, thread, mapped)
            if (a.mayChange) a.addChangeListener(this)
            if (b.mayChange) b.addChangeListener(this)
        } else {
            if (a.mayChange) a.removeChangeListener(this)
            if (b.mayChange) b.removeChangeListener(this)
            valueUpdater<T>().eagerOrLazySet(this, thread, this as T)
        }
    }

    private companion object {
        @JvmField
        val ValueUpdater: AtomicReferenceFieldUpdater<BiMappedProperty<*, *, *>, Any?> =
                AtomicReferenceFieldUpdater.newUpdater(BiMappedProperty::class.java, Any::class.java, "valueRef")

        @Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
        inline fun <T> valueUpdater() =
                ValueUpdater as AtomicReferenceFieldUpdater<BiMappedProperty<*, *, T>, T>
    }

}
