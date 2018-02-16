package net.aquadc.properties.internal

import net.aquadc.properties.ChangeListener
import net.aquadc.properties.Property
import java.util.concurrent.CopyOnWriteArrayList


class ConcDistinctPropertyWrapper<out T>(
        private val original: Property<T>,
        private val areEqual: (T, T) -> Boolean
) : BaseConcProperty<T>() {

    init {
        if (!original.mayChange)
            throw IllegalArgumentException("wrapping immutable property is useless")

        original.addChangeListener { old, new ->
            if (!areEqual(old, new)) {
                listeners.forEach { it(old, new) }
            }
        }
    }

    override val value: T
        get() = original.value

    private var listeners = CopyOnWriteArrayList<ChangeListener<T>>()
    override fun addChangeListener(onChange: (old: T, new: T) -> Unit) {
        listeners.add(onChange)
    }
    override fun removeChangeListener(onChange: (old: T, new: T) -> Unit) {
        listeners.remove(onChange)
    }

}
