package net.aquadc.properties.internal

import net.aquadc.properties.Property


class ConcDistinctPropertyWrapper<out T>(
        private val original: Property<T>,
        areEqual: (T, T) -> Boolean
) : ConcPropListeners<T>() {

    init {
        if (!original.mayChange)
            throw IllegalArgumentException("wrapping immutable property is useless")

        original.addChangeListener { old, new ->
            if (!areEqual(old, new)) {
                valueChanged(old, new)
            }
        }
    }

    override fun getValue(): T =
            original.getValue()

}
