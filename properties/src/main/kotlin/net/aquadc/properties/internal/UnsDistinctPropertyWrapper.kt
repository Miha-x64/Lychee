package net.aquadc.properties.internal

import net.aquadc.properties.Property


class UnsDistinctPropertyWrapper<out T>(
        private val original: Property<T>,
        private val areEqual: (T, T) -> Boolean
) : UnsListeners<T>() {

    init {
        check(original.mayChange)

        original.addChangeListener { old, new ->
            if (!areEqual(old, new)) {
                listeners.notifyAll(old, new)
            }
        }
    }

    override val value: T
        get() {
            checkThread()
            return original.value
        }

}
