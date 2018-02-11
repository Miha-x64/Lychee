package net.aquadc.properties.internal

import net.aquadc.properties.Property


class UnsynchronizedMappedReferenceProperty<in O, out T>(
        original: Property<O>,
        private val transform: (O) -> T
) : Property<T> {

    private val thread = Thread.currentThread()

    init {
        if (original is ImmutableReferenceProperty)
            throw IllegalArgumentException("immutable property $original should not be mapped")
    }

    private var listeners: Any? = null

    private var valueRef: T = transform(original.value)

    init {
        original.addChangeListener { old, new ->
            val tOld = transform(old)
            val tNew = transform(new)
            valueRef = tNew
            listeners.notifyAll(tOld, tNew)
        }
    }

    override val value: T get() {
        checkThread(thread)
        return valueRef
    }

    override val mayChange: Boolean get() {
        checkThread(thread)
        return true
    }

    override val isConcurrent: Boolean get() {
        checkThread(thread)
        return false
    }

    override fun addChangeListener(onChange: (old: T, new: T) -> Unit) {
        checkThread(thread)
        listeners = listeners.plus(onChange)
    }

    override fun removeChangeListener(onChange: (old: T, new: T) -> Unit) {
        checkThread(thread)
        listeners = listeners.minus(onChange)
    }

}
