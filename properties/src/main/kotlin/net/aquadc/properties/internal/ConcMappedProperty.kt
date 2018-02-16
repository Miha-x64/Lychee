package net.aquadc.properties.internal

import net.aquadc.properties.Property
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater


class ConcMappedProperty<in O, out T>(
        original: Property<O>,
        private val transform: (O) -> T
) : BaseConcProperty<T>() {

    init {
        check(original.isConcurrent)
        check(original.mayChange)
    }

    @Volatile @Suppress("UNUSED")
    private var valueRef = transform(original.value)

    init {
        original.addChangeListener { _, new ->
            val newRef = transform(new)
            val oldRef = valueUpdater<T>().getAndSet(this, newRef)
            listeners.forEach { it(oldRef, newRef) }
        }
    }

    override val value: T
        get() = valueUpdater<T>().get(this)

    private val listeners = CopyOnWriteArrayList<(T, T) -> Unit>()
    override fun addChangeListener(onChange: (old: T, new: T) -> Unit) {
        listeners.add(onChange)
    }
    override fun removeChangeListener(onChange: (old: T, new: T) -> Unit) {
        listeners.remove(onChange)
    }

    @Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
    private companion object {
        val ValueUpdater: AtomicReferenceFieldUpdater<ConcMappedProperty<*, *>, Any?> =
                AtomicReferenceFieldUpdater.newUpdater(ConcMappedProperty::class.java, Any::class.java, "valueRef")

        inline fun <T> valueUpdater() =
                ValueUpdater as AtomicReferenceFieldUpdater<ConcMappedProperty<*, T>, T>
    }

}
