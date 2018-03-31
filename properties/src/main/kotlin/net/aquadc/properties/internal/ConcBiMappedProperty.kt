package net.aquadc.properties.internal

import net.aquadc.properties.Property
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater


internal class ConcBiMappedProperty<in A, in B, out T>(
        a: Property<A>,
        b: Property<B>,
        private val transform: (A, B) -> T
) : ConcPropNotifier<T>() {

    init {
        check(a.isConcurrent)
        check(b.isConcurrent)
        check(a.mayChange)
        check(b.mayChange)
    }

    @Volatile @Suppress("UNUSED")
    private var valueRef = transform(a.value, b.value)
    init {
        a.addChangeListener { _, new -> recalculate(new, b.value) }
        b.addChangeListener { _, new -> recalculate(a.value, new) }
    }

    override val value: T
        get() = valueUpdater<T>().get(this)

    @Suppress("MemberVisibilityCanBePrivate") // produce no access$
    internal fun recalculate(newA: A, newB: B) {
        val new = transform(newA, newB)
        val old = valueUpdater<T>().getAndSet(this, new)
        valueChanged(old, new, null)
    }

    private companion object {
        @JvmField
        val ValueUpdater: AtomicReferenceFieldUpdater<ConcBiMappedProperty<*, *, *>, Any?> =
                AtomicReferenceFieldUpdater.newUpdater(ConcBiMappedProperty::class.java, Any::class.java, "valueRef")

        @Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
        inline fun <T> valueUpdater() =
                ValueUpdater as AtomicReferenceFieldUpdater<ConcBiMappedProperty<*, *, T>, T>
    }

}
