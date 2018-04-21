package net.aquadc.properties.diff.internal

import net.aquadc.properties.diff.MutableDiffProperty
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater


@PublishedApi
internal class ConcMutableDiffProperty<T, D>(
        value: T
) : ConcDiffPropNotifier<T, D>(), MutableDiffProperty<T, D> {

    @Volatile @Suppress("UNUSED")
    private var valueRef: T = value

    override val value: T
        get() = valueUpdater<T, D>().get(this)

    override fun casValue(expectValue: T, newValue: T, diff: D): Boolean {
        return if (valueUpdater<T, D>().compareAndSet(this, expectValue, newValue)) {
            valueChanged(expectValue, newValue, diff)
            // updateListeners will be called then
            true
        } else {
            false
        }
    }

    @Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
    private companion object {
        @JvmField val valueUpdater: AtomicReferenceFieldUpdater<*, *> =
                AtomicReferenceFieldUpdater.newUpdater(ConcMutableDiffProperty::class.java, Any::class.java, "valueRef")

        inline fun <T, D> valueUpdater() =
                valueUpdater as AtomicReferenceFieldUpdater<ConcMutableDiffProperty<T, D>, T>
    }

}
