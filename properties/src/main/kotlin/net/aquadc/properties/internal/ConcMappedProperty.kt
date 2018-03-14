package net.aquadc.properties.internal

import net.aquadc.properties.Property
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater


class ConcMappedProperty<in O, out T>(
        original: Property<O>,
        private val transform: (O) -> T
) : ConcPropListeners<T>() {

    init {
        check(original.isConcurrent)
        check(original.mayChange)
    }

    @Volatile @Suppress("UNUSED")
    private var valueRef = transform(original.getValue())

    init {
        original.addChangeListener { _, new ->
            val newRef = transform(new)
            val oldRef = valueUpdater<T>().getAndSet(this, newRef)
            valueChanged(oldRef, newRef)
        }
    }

    override fun getValue(): T =
            valueUpdater<T>().get(this)

    @Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
    private companion object {
        val ValueUpdater: AtomicReferenceFieldUpdater<ConcMappedProperty<*, *>, Any?> =
                AtomicReferenceFieldUpdater.newUpdater(ConcMappedProperty::class.java, Any::class.java, "valueRef")

        inline fun <T> valueUpdater() =
                ValueUpdater as AtomicReferenceFieldUpdater<ConcMappedProperty<*, T>, T>
    }

}
