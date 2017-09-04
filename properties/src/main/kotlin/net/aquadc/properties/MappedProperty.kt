package net.aquadc.properties

import java.util.concurrent.CopyOnWriteArrayList

class MappedProperty<O, out T>(
        private val original: Property<O>,
        private val transform: (O) -> T
) : Property<T> {

    private val listeners = CopyOnWriteArrayList<(T, T) -> Unit>()

    init {
        original.addChangeListener { old, new ->
            listeners.forEach { it(transform(old), transform(new)) }
        }
    }

    override val value: T
        get() = transform(original.value)

    override fun addChangeListener(onChange: (old: T, new: T) -> Unit) {
        listeners.add(onChange)
    }

    override fun removeChangeListener(onChange: (old: T, new: T) -> Unit) {
        listeners.remove(onChange)
    }

}