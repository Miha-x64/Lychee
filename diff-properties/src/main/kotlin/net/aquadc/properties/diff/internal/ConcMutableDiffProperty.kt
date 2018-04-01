package net.aquadc.properties.diff.internal

import net.aquadc.properties.ChangeListener
import net.aquadc.properties.diff.DiffChangeListener
import net.aquadc.properties.diff.MutableDiffProperty
import net.aquadc.properties.internal.ConcPropListeners
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater


class ConcMutableDiffProperty<T, D>(
        value: T
) : ConcPropListeners<T, D, Any, Pair<T, D>>(), MutableDiffProperty<T, D> {

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

    override fun addChangeListener(onChange: ChangeListener<T>) =
            listenersUpdater().update(this) { it.withListener(onChange) }

    override fun addChangeListener(onChangeWithDiff: DiffChangeListener<T, D>) =
            listenersUpdater().update(this) { it.withListener(onChangeWithDiff) }

    override fun removeChangeListener(onChange: ChangeListener<T>) =
            listenersUpdater().update(this) { it.withoutListener(onChange) }

    override fun removeChangeListener(onChangeWithDiff: DiffChangeListener<T, D>) =
            listenersUpdater().update(this) { it.withoutListener(onChangeWithDiff) }

    override fun pack(new: T, diff: D): Pair<T, D> =
            new to diff

    override fun unpackValue(packed: Pair<T, D>): T =
            packed.first

    override fun unpackDiff(packed: Pair<T, D>): D =
            packed.second

    @Suppress("UNCHECKED_CAST") // oh, so many of them
    override fun notify(listener: Any, old: T, new: T, diff: D) = when (listener) {
        is Function2<*, *, *> -> (listener as ChangeListener<T>)(old, new)
        is Function3<*, *, *, *> -> (listener as DiffChangeListener<T, D>)(old, new, diff)
        else -> throw AssertionError()
    }

    @Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
    private companion object {
        @JvmField val valueUpdater: AtomicReferenceFieldUpdater<*, *> =
                AtomicReferenceFieldUpdater.newUpdater(ConcMutableDiffProperty::class.java, Any::class.java, "valueRef")

        inline fun <T, D> valueUpdater() =
                valueUpdater as AtomicReferenceFieldUpdater<ConcMutableDiffProperty<T, D>, T>
    }

}
