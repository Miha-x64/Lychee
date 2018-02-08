package net.aquadc.properties.internal

import net.aquadc.properties.Property
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater


class ConcurrentMappedReferenceProperty<in O, out T>(
        original: Property<O>,
        private val transform: (O) -> T
) : Property<T> {

    init {
        if (original is ImmutableReferenceProperty)
            throw IllegalArgumentException("immutable property $original should not be mapped")
    }

    @Volatile @Suppress("UNUSED")
    private var valueRef = transform(original.value)
    private val listeners = CopyOnWriteArrayList<(T, T) -> Unit>()

    init {
        original.addChangeListener { _, new ->
            val newRef = transform(new)
            val oldRef = valueUpdater<T>().getAndSet(this, newRef)
            listeners.forEach { it(oldRef, newRef) }
        }
    }

    override val value: T
        get() = valueUpdater<T>().get(this)

    override val mayChange: Boolean get() = true
    override val isConcurrent: Boolean get() = true

    override fun addChangeListener(onChange: (old: T, new: T) -> Unit) {
        listeners.add(onChange)
    }

    override fun removeChangeListener(onChange: (old: T, new: T) -> Unit) {
        listeners.remove(onChange)
    }

    @Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
    private companion object {
        val ValueUpdater: AtomicReferenceFieldUpdater<ConcurrentMappedReferenceProperty<*, *>, Any?> =
                AtomicReferenceFieldUpdater.newUpdater(ConcurrentMappedReferenceProperty::class.java, Any::class.java, "valueRef")

        inline fun <T> valueUpdater() =
                ValueUpdater as AtomicReferenceFieldUpdater<ConcurrentMappedReferenceProperty<*, T>, T>
    }

}
