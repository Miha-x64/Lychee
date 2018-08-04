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
) : `Notifier+1AtomicRef`<T, Any?>(
        true, value
), MutableProperty<T>, ChangeListener<T> {

    override var value: T // field: T | Binding<T> | rebinding()
        get() {
            var value: Any?
            do { value = valueUpdater().get(this) } while (value === rebinding())
            return if (value is Binding<*>) (value as Binding<T>).original.value else value as T
        }
        set(newValue) {
            while (!casValue(value, newValue)) Thread.yield()
        }

    override fun bindTo(sample: Property<T>) {
        val newValue: Any?
        val newSample: Property<T>?
        if (sample.mayChange) {
            newValue = Binding(sample)
            newSample = sample
        } else {
            newValue = sample.value
            newSample = null
        }

        var oldValue: Any?
        while (true) {
            oldValue = valueUpdater().getAndSet(this, rebinding())
            if (oldValue === rebinding()) Thread.yield() // other thread rebinding this property, wait
            else break
        }
        // under mutex

        // fixme: potential concurrent bug, #40
        if ((oldValue !is Binding<*> || oldValue.original !== newSample) && isBeingObserved()) {
            (oldValue as? Binding<T>)?.original?.removeChangeListener(this)
            newSample?.addUnconfinedChangeListener(this)
        }
        // end mutex
        check(valueUpdater().getAndSet(this, newValue) === rebinding())

        val prevValue = if (oldValue is Binding<*>) (oldValue as Binding<T>).original.value else oldValue as T
        val newValueValue = if (newValue is Binding<*>) (newValue as Binding<T>).original.value else newValue as T

        valueChanged(prevValue, newValueValue, null)
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
        if (prevValOrBind === rebinding()) return false

        val prevValue: T
        val realExpect: Any?
        if (prevValOrBind is Binding<*>) {
            prevValOrBind as Binding<T>
            if (!valueUpdater().compareAndSet(this, prevValOrBind, rebinding())) return false

            // under mutex
            prevValue = prevValOrBind.original.value
            if (prevValue !== expect) {
                // under mutex (no update from Sample allowed) we understand that sample's value !== expected
                check(valueUpdater().compareAndSet(this, rebinding(), prevValOrBind))
                // so just revert and report failure
                return false
            }
            realExpect = rebinding()
            prevValOrBind.original.removeChangeListener(this)
        } else {
            prevValue = prevValOrBind as T
            realExpect = expect
        }

        val success = valueUpdater().compareAndSet(this, realExpect, update) // end mutex
        if (success) {
            valueChanged(prevValue, update, null)
        } else if (realExpect === rebinding()) {
            // if we're set a mutex, we must release it; if it is not set, there's a program error
            throw AssertionError()
        }
        return success
    }

    override fun invoke(old: T, new: T) {
        while (value === rebinding()) Thread.yield()
        valueChanged(old, new, null)
    }

    private class Binding<T>(val original: Property<T>)

    @Suppress("NOTHING_TO_INLINE")
    private inline fun rebinding() = Unset

    @Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
    private inline fun valueUpdater() =
            refUpdater() as AtomicReferenceFieldUpdater<`ConcMutable-`<T>, Any?>

}
