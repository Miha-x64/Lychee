package net.aquadc.properties.internal

import net.aquadc.properties.ChangeListener
import net.aquadc.properties.Property
import net.aquadc.properties.addUnconfinedChangeListener

@PublishedApi
internal class `Distinct-`<out T>(
        private val original: Property<T>,
        private val areEqual: (T, T) -> Boolean
) : `-Notifier`<T>(threadIfNot(original.isConcurrent)), ChangeListener<@UnsafeVariance T> {

    init {
        check(original.mayChange)
    }

    override val value: T
        get() {
            if (thread != null) checkThread()
            return original.value
        }

    override fun invoke(old: @UnsafeVariance T, new: @UnsafeVariance T) {
        if (!areEqual(old, new)) {
            valueChanged(old, new, null)
        }
    }

    override fun observedStateChanged(observed: Boolean) {
        if (observed) {
            original.addUnconfinedChangeListener(this)
        } else {
            original.removeChangeListener(this)
        }
    }

}
