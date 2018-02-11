package net.aquadc.properties.internal

import net.aquadc.properties.Property


class UnsDistinctPropertyWrapper<out T>(
        private val original: Property<T>,
        private val areEqual: (T, T) -> Boolean
) : Property<T> {

    init {
        if (!original.mayChange)
            throw IllegalArgumentException("wrapping immutable property is useless")

        original.addChangeListener { old, new ->
            if (!areEqual(old, new)) {
                listeners.notifyAll(old, new)
            }
        }
    }

    private val thread = Thread.currentThread()

    override val value: T
        get() {
            checkThread(thread)
            return original.value
        }

    override val mayChange: Boolean
        get() {
            checkThread(thread)
            return true
        }

    override val isConcurrent: Boolean
        get() {
            checkThread(thread)
            return false
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
