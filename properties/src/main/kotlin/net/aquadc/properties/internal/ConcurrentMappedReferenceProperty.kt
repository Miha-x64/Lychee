package net.aquadc.properties.internal

import net.aquadc.properties.Property
import java.util.concurrent.CopyOnWriteArrayList

class ConcurrentMappedReferenceProperty<O, out T>(
        private val original: Property<O>,
        private val transform: (O) -> T
) : Property<T> {

    init {
        if (original is ImmutableReferenceProperty)
            throw IllegalArgumentException("immutable property $original should not be mapped")
    }

    private val listeners = CopyOnWriteArrayList<(T, T) -> Unit>()

    init {
        original.addChangeListener { old, new ->
            val tOld = transform(old)
            val tNew = transform(new)
            if (tOld !== tNew) {
                listeners.forEach { it(transform(old), transform(new)) }
            }
        }
    }

    override val value: T
        get() = transform(original.value)

    override val mayChange: Boolean get() = true

    override fun addChangeListener(onChange: (old: T, new: T) -> Unit) {
        listeners.add(onChange)
    }

    override fun removeChangeListener(onChange: (old: T, new: T) -> Unit) {
        listeners.remove(onChange)
    }

}
