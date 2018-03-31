package net.aquadc.properties.internal

import net.aquadc.properties.MutableProperty
import net.aquadc.properties.Property

/**
 * Single-threaded mutable property.
 * Will remember thread it was created on and throw an exception if touched from wrong thread.
 */
class UnsMutableProperty<T>(
        value: T
) : UnsListeners<T>(), MutableProperty<T> {

    private var _notifyAllFunc: ((T, T) -> Unit)? = null
    private val notifyAllFunc: (T, T) -> Unit = _notifyAllFunc ?: ::valueChanged.also { _notifyAllFunc = it }

    var valueRef: T = value
    override fun getValue(): T {
        checkThread()
        val sample = sample
        return if (sample == null) valueRef else sample.getValue()
    }

    override fun setValue(newValue: T) {
        checkThread()
        dropBinding()

        // set value then
        val old = valueRef
        valueRef = newValue

        valueChanged(old, newValue)
    }

    private var sample: Property<T>? = null

    override fun bindTo(sample: Property<T>) {
        checkThread()
        val newSample = if (sample.mayChange) sample else null
        val oldSample = this.sample
        this.sample = newSample
        oldSample?.removeChangeListener(notifyAllFunc)
        newSample?.addChangeListener(notifyAllFunc)

        val old = valueRef
        val new = sample.getValue()
        valueRef = new
        valueChanged(old, new)
    }

    override fun casValue(expect: T, update: T): Boolean {
        checkThread()
        dropBinding()
        return if (valueRef === expect) {
            valueRef = update
            valueChanged(expect, update)
            true
        } else {
            false
        }
    }

    private fun dropBinding() {
        val oldSample = sample
        oldSample?.removeChangeListener(notifyAllFunc)
        sample = null
    }

}
