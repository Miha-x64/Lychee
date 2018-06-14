package net.aquadc.properties.internal

import net.aquadc.properties.ChangeListener
import net.aquadc.properties.MutableProperty
import net.aquadc.properties.Property
import net.aquadc.properties.addUnconfinedChangeListener
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater
import kotlin.concurrent.write

/**
 * Concurrent [MutableProperty] implementation.
 */
@PublishedApi
internal class ConcMutableProperty<T>(
        value: T
) : PropNotifier<T>(null), MutableProperty<T>, ChangeListener<T> {

    @Volatile @Suppress("UNUSED")
    private var valueRef: Value<T> = Value.Reference(value)

    override var value: T
        get() = valueUpdater<T>().get(this).value
        set(newValue) {
            val old: T = valueUpdater<T>().getAndSet(this, Value.Reference(newValue)).value
            valueChanged(old, newValue, null)
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

        var oldT: T? = null
        subscriptionLock.write {
            val oldValue = valueUpdater<T>().getAndSet(this, newValue)
            oldT = oldValue.value

            if (oldValue is Value.Binding && oldValue.original === newSample) {
                // nothing changed, don't re-subscribe, don't notify
                return
            }

            if (isBeingObserved()) {
                (oldValue as? Value.Binding)?.original?.removeChangeListener(this)
                newSample?.addUnconfinedChangeListener(this)
            }
        }

        valueChanged(oldT as T, newValue.value, null)
    }

    override fun observedStateChangedWLocked(observed: Boolean) {
        val value = valueUpdater<T>().get(this)
        if (value !is Value.Binding) return

        if (observed) value.original.addUnconfinedChangeListener(this)
        else value.original.removeChangeListener(this)
    }

    override fun casValue(expect: T, update: T): Boolean {
        val actual = valueUpdater<T>().get(this)
        return if (actual.value === expect && valueUpdater<T>().compareAndSet(this, actual, Value.Reference(update))) {
            valueChanged(expect, update, null)
            true
        } else {
            false
        }
    }

    override fun invoke(old: T, new: T) {
        valueChanged(old, new, null)
    }

    private sealed class Value<T> {
        abstract val value: T

        class Reference<T>(override val value: T) : Value<T>()

        class Binding<T>(val original: Property<T>) : Value<T>() {
            override val value: T
                get() = original.value
        }
    }

    @Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST") // just safe unchecked cast, should produce no bytecode
    private companion object {
        @JvmField
        val ValueUpdater: AtomicReferenceFieldUpdater<ConcMutableProperty<*>, Value<*>> =
                AtomicReferenceFieldUpdater.newUpdater(ConcMutableProperty::class.java, Value::class.java, "valueRef")

        inline fun <T> valueUpdater() =
                ValueUpdater as AtomicReferenceFieldUpdater<ConcMutableProperty<T>, Value<T>>
    }

}
