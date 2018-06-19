package net.aquadc.properties.internal

import net.aquadc.properties.ChangeListener
import net.aquadc.properties.MutableProperty
import net.aquadc.properties.Property

/**
 * Single-threaded mutable property.
 * Will remember thread it was created on and throw an exception if touched from wrong thread.
 */
@PublishedApi
internal class `UnsMutable*`<T>(
        value: T
) : PropNotifier<T>(Thread.currentThread()), MutableProperty<T>, ChangeListener<T> {

    var valueRef: T = value

    override var value: T
        get() {
            checkThread()
            val sample = sample
            return if (sample == null) valueRef else sample.value
        }
        set(newValue) {
            checkThread()
            dropBinding()

            // set value then
            val old = valueRef
            valueRef = newValue

            valueChanged(old, newValue, null)
        }

    private var sample: Property<T>? = null

    override fun bindTo(sample: Property<T>) {
        checkThread()
        val newSample = if (sample.mayChange) sample else null
        val oldSample = this.sample
        this.sample = newSample

        if (isBeingObserved()) {
            oldSample?.removeChangeListener(this)
            newSample?.addChangeListener(this)
        }

        val old = valueRef
        val new = sample.value
        valueRef = new
        valueChanged(old, new, null)
    }

    override fun observedStateChanged(observed: Boolean) {
        val sample = sample ?: return

        if (observed) sample.addChangeListener(this)
        else sample.removeChangeListener(this)
    }

    override fun casValue(expect: T, update: T): Boolean {
        checkThread()
        dropBinding()
        return if (valueRef === expect) {
            valueRef = update
            valueChanged(expect, update, null)
            true
        } else {
            false
        }
    }

    private fun dropBinding() {
        val oldSample = sample ?: return

        if (isBeingObserved()) {
            oldSample.removeChangeListener(this)
        }
        sample = null
    }

    override fun invoke(old: T, new: T) {
        valueChanged(old, new, null)
    }

}
