package net.aquadc.properties.internal

import net.aquadc.properties.Property
import net.aquadc.properties.executor.Worker
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater


class ConcMappedProperty<in O, out T>(
        original: Property<O>,
        map: (O) -> T,
        mapOn: Worker
) : ConcPropListeners<T>() {

    init {
        check(original.isConcurrent)
        check(original.mayChange)
    }

    @Volatile @Suppress("UNUSED")
    private var valueRef = map(original.value)

    init {
        val onValueMapped = { new: T ->
            val old = valueUpdater<T>().getAndSet(this, new)
            valueChanged(old, new)
        }

        original.addChangeListener { _, new ->
            mapOn.map(new, map, onValueMapped)
        }
    }

    override val value: T
        get() = valueUpdater<T>().get(this)

    private companion object {
        @JvmField
        val ValueUpdater: AtomicReferenceFieldUpdater<ConcMappedProperty<*, *>, Any?> =
                AtomicReferenceFieldUpdater.newUpdater(ConcMappedProperty::class.java, Any::class.java, "valueRef")

        @Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
        inline fun <T> valueUpdater() =
                ValueUpdater as AtomicReferenceFieldUpdater<ConcMappedProperty<*, T>, T>
    }

}
