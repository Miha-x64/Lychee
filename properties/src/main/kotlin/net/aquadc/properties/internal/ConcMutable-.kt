package net.aquadc.properties.internal

import net.aquadc.properties.ChangeListener
import net.aquadc.properties.MutableProperty
import net.aquadc.properties.Property
import net.aquadc.properties.addUnconfinedChangeListener
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater

/**
 * Concurrent [MutableProperty] implementation.
 */
@PublishedApi
internal class `ConcMutable-`<T>(
        value: T
) : `Notifier-1AtomicRef`<T, Any?>(
        true, value
), MutableProperty<T>, ChangeListener<T> {

    override var value: T // field: T | Binding<T>
        get() {
            val value = valueUpdater().get(this)
            return if (value is Binding<*>) {
                value as Binding<T>
                val retValue: T
                if (isBeingObserved()) {
                    value.ourValue
                } else {
                    retValue = value.original.value // we're not observed, so stale value may be remembered — peek a fresh one
                    retValue
                }
            } else {
                value as T
            }
        }
        set(newValue) {
            while (!casValue(value, newValue)) Thread.yield()
        }

    override fun bindTo(sample: Property<T>) {
        val newValOrBinding: Any? = if (sample.mayChange) Binding(sample, sample.value) else sample.value
        val oldValOrBinding = valueUpdater().get(this)

        val prevValue = if (oldValOrBinding is Binding<*>) (oldValOrBinding as Binding<T>).original.value else oldValOrBinding as T
        val newValue = if (newValOrBinding is Binding<*>) (newValOrBinding as Binding<T>).original.value else newValOrBinding as T

        withLockedTransition { // 'observed' state should not be changed concurrently
            if (isBeingObserved()) {
                (oldValOrBinding as? Binding<T>)?.original?.removeChangeListener(this)
                (newValOrBinding as? Binding<T>)?.original?.addUnconfinedChangeListener(this)
            }
            (newValOrBinding as? Binding<T>)?.ourValue = newValue
            valueUpdater().set(this, newValOrBinding)
        }

        valueChanged(prevValue, newValue, null)
    }

    override fun observedStateChanged(observed: Boolean) {
        val value = valueUpdater().get(this)
        if (value !is Binding<*>) return

        value as Binding<T>
        if (observed) value.original.addUnconfinedChangeListener(this)
        else value.original.removeChangeListener(this)
    }

    override fun casValue(expect: T, update: T): Boolean {
        val prevValOrBind = valueUpdater().get(this)

        val prevValue: T
        if (prevValOrBind is Binding<*>) {
            prevValOrBind as Binding<T>
            if (!tryLockTransition()) return false

            // under mutex
            prevValue = if (isBeingObserved()) prevValOrBind.ourValue else prevValOrBind.original.value
            // if not, just peek a fresh value, may be inexact, but it's OK, because no one have received these updates

            if (prevValue != expect) {
                // under mutex (no update from Sample allowed) we understand that sample's value != expected
                unlockTransition()
                // so just report failure
                return false
            }

            prevValOrBind.original.removeChangeListener(this)
            unlockTransition()
        } else {
            prevValue = prevValOrBind as T
            if (prevValue != expect) return false
        }

        val success = valueUpdater().compareAndSet(this, prevValOrBind, update)
        if (success) {
            valueChanged(prevValue, update, null)
        }
        return success
    }

    override fun invoke(old: T, new: T) {
        withLockedTransition {
            (valueUpdater().get(this) as? Binding<T>)?.ourValue = new
            valueChanged(old, new, null)
        }
    }

    private class Binding<T>(val original: Property<T>, @Volatile var ourValue: T)

    @Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
    private inline fun valueUpdater() =
            refUpdater() as AtomicReferenceFieldUpdater<`ConcMutable-`<T>, Any?>

}
