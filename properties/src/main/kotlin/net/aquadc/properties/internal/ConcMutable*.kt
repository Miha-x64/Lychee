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
internal class `ConcMutable*`<T>(
        value: T
) : PropNotifier<T>(null), MutableProperty<T>, ChangeListener<T> {

    @Volatile @Suppress("UNUSED")
    private var valueRef: Value<T> = Value.Reference(value)

    override var value: T
        get() = valueUpdater<T>().get(this).value
        set(newValue) {
            val next: Value<T> = Value.Reference(newValue)
            var prev: Value<T>
            do {
                while (true) {
                    prev = valueUpdater<T>().get(this)
                    if (prev === Value.Rebinding) Thread.yield()
                    else break
                }
            } while (!valueUpdater<T>().compareAndSet(this, prev, next))

            if (prev is Value.Binding) {
                prev.original.removeChangeListener(this)
            }

            valueChanged(prev.value, newValue, null)
        }

    override fun bindTo(sample: Property<T>) {
        val newValue: Value<T>
        val newSample: Property<T>?
        if (sample.mayChange) {
            newValue = Value.Binding(sample)
            newSample = sample
        } else {
            newValue = Value.Reference(sample.value)
            newSample = null
        }

        var oldValue: Value<T>
        while (true) {
            oldValue = valueUpdater<T>().getAndSet(this, Value.Rebinding)
            if (oldValue == Value.Rebinding) Thread.yield() // other thread rebinding this property, wait
            else break
        }
        // under mutex

        if ((oldValue !is Value.Binding || oldValue.original !== newSample) && isBeingObserved()) {
            (oldValue as? Value.Binding)?.original?.removeChangeListener(this)
            newSample?.addUnconfinedChangeListener(this)
        }
        // end mutex
        check(valueUpdater<T>().getAndSet(this, newValue) === Value.Rebinding)

        valueChanged(oldValue.value, newValue.value, null)
    }

    override fun observedStateChanged(observed: Boolean) {
        val value = valueUpdater<T>().get(this)
        if (value !is Value.Binding) return

        if (observed) value.original.addUnconfinedChangeListener(this)
        else value.original.removeChangeListener(this)
    }

    override fun casValue(expect: T, update: T): Boolean {
        val actual = valueUpdater<T>().get(this)
        return if (actual !== Value.Rebinding
                && actual.value === expect
                && valueUpdater<T>().compareAndSet(this, actual, Value.Reference(update))) {
            valueChanged(expect, update, null)
            true
        } else {
            false
        }
    }

    override fun invoke(old: T, new: T) {
        valueChanged(old, new, null)
    }

    private sealed class Value<out T> {
        abstract val value: T

        class Reference<T>(override val value: T) : Value<T>()

        class Binding<T>(val original: Property<T>) : Value<T>() {
            override val value: T
                get() = original.value
        }

        object Rebinding : Value<Nothing>() {
            override val value: Nothing
                get() = throw UnsupportedOperationException()
        }
    }

    private companion object {
        @JvmField internal val ValueUpdater: AtomicReferenceFieldUpdater<`ConcMutable*`<*>, Value<*>> =
                AtomicReferenceFieldUpdater.newUpdater(`ConcMutable*`::class.java, Value::class.java, "valueRef")

        @Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
        private inline fun <T> valueUpdater() =
                ValueUpdater as AtomicReferenceFieldUpdater<`ConcMutable*`<T>, Value<T>>
    }

}
