package net.aquadc.properties.internal

import net.aquadc.properties.Property


class UnsDistinctPropertyWrapper<out T>(
        private val original: Property<T>,
        areEqual: (T, T) -> Boolean
) : UnsListeners<T>() {

    init {
        check(original.mayChange)

        original.addChangeListener { old, new ->
            if (!areEqual(old, new)) {
                valueChanged(old, new)
            }
        }
    }

    override val value: T
        get() {
            checkThread()
            return original.value
        }

}
