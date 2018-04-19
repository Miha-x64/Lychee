package net.aquadc.properties.internal

import net.aquadc.properties.Property

@PublishedApi
internal class DistinctPropertyWrapper<out T>(
        private val original: Property<T>,
        areEqual: (T, T) -> Boolean
) : PropNotifier<T>(threadIfNot(original.isConcurrent)) {

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
            if (thread != null) checkThread()
            return original.value
        }

}
