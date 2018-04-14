package net.aquadc.properties.internal

import net.aquadc.properties.Property


class UnsDistinctPropertyWrapper<out T>(
        private val original: Property<T>,
        areEqual: (T, T) -> Boolean
) : PropNotifier<T>(Thread.currentThread()) {

    init {
        check(original.mayChange)

        original.addChangeListener { old, new ->
            if (!areEqual(old, new)) {
                valueChanged(old, new, null)
            }
        }
    }

    override val value: T
        get() {
            checkThread()
            return original.value
        }

}
