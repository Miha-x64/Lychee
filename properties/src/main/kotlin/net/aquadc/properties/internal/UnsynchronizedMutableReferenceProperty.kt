package net.aquadc.properties.internal

import net.aquadc.properties.MutableProperty
import net.aquadc.properties.Property


class UnsynchronizedMutableReferenceProperty<T>(
        value: T
) : MutableProperty<T> {

    private val thread = Thread.currentThread()

    var valueRef: T = value
    override var value: T
        get() {
            checkThread(thread)
            val sample = sample
            return if (sample == null) valueRef else sample.value
        }
        set(new) {
            checkThread(thread)
            val old = valueRef
            valueRef = new

            // if bound, unbind
            val oldSample = sample
            oldSample?.removeChangeListener(onChangeInternal)
            sample = null

            onChangeInternal(old, new)
        }

    private var sample: Property<T>? = null

    override val mayChange: Boolean get() {
        checkThread(thread)
        return true
    }

    override val isConcurrent: Boolean get() {
        checkThread(thread)
        return false
    }

    override fun bindTo(sample: Property<T>) {
        checkThread(thread)
        val newSample = if (sample.mayChange) sample else null
        val oldSample = this.sample
        this.sample = newSample
        oldSample?.removeChangeListener(onChangeInternal)
        newSample?.addChangeListener(onChangeInternal)

        val old = valueRef
        val new = sample.value
        valueRef = new
        onChangeInternal(old, new)
    }

    private val onChangeInternal: (T, T) -> Unit = this::onChangeInternal
    private fun onChangeInternal(old: T, new: T) {
        if (new !== old) {
            listeners.notifyAll(old, new)
        }
    }

    private var listeners: Any? = null

    override fun addChangeListener(onChange: (old: T, new: T) -> Unit) {
        checkThread(thread)
        listeners = listeners.plus(onChange)
    }

    override fun removeChangeListener(onChange: (old: T, new: T) -> Unit) {
        checkThread(thread)
        listeners = listeners.minus(onChange)
    }

}
