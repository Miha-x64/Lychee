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
    private var valueRef = transform(a.value, b.value)
    init {
        check(a.mayChange)
        check(b.mayChange)

        a.addChangeListener(this)
        b.addChangeListener(this)
    }

    override val value: T
        get() {
            if (thread !== null) checkThread()
            return valueUpdater<T>().get(this)
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

    private companion object {
        @JvmField
        val ValueUpdater: AtomicReferenceFieldUpdater<BiMappedProperty<*, *, *>, Any?> =
                AtomicReferenceFieldUpdater.newUpdater(BiMappedProperty::class.java, Any::class.java, "valueRef")

        @Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
        inline fun <T> valueUpdater() =
                ValueUpdater as AtomicReferenceFieldUpdater<BiMappedProperty<*, *, T>, T>
    }

}
