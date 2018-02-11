package net.aquadc.properties.internal

import net.aquadc.properties.Property
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater


internal class ConcurrentBiMappedCachedReferenceProperty<in A, in B, out T>(
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

    override val mayChange: Boolean get() = true
    override val isConcurrent: Boolean get() = true

    @Volatile @Suppress("UNUSED")
    private var valueRef = transform(a.value, b.value)
    init {
        a.addChangeListener { _, new -> recalculate(new, b.value) }
        b.addChangeListener { _, new -> recalculate(a.value, new) }
    }

    override val value: T
        get() = valueUpdater<T>().get(this)

    private val listeners = CopyOnWriteArrayList<(T, T) -> Unit>()

    private fun recalculate(newA: A, newB: B) {
        val new = transform(newA, newB)
        val old = valueUpdater<T>().getAndSet(this, new)
        listeners.forEach { it(old, new) }
    }

    override fun addChangeListener(onChange: (old: T, new: T) -> Unit) {
        listeners.add(onChange)
    }

    override fun removeChangeListener(onChange: (old: T, new: T) -> Unit) {
        listeners.remove(onChange)
    }

    @Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
    private companion object {
        val ValueUpdater: AtomicReferenceFieldUpdater<ConcurrentBiMappedCachedReferenceProperty<*, *, *>, Any?> =
                AtomicReferenceFieldUpdater.newUpdater(ConcurrentBiMappedCachedReferenceProperty::class.java, Any::class.java, "valueRef")

        inline fun <T> valueUpdater() =
                ValueUpdater as AtomicReferenceFieldUpdater<ConcurrentBiMappedCachedReferenceProperty<*, *, T>, T>
    }

}
