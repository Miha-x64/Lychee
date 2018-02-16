package net.aquadc.properties.internal

import net.aquadc.properties.ChangeListener
import net.aquadc.properties.Property

/**
 * Base class for unsynchronzed properties.
 * I don't like implementation inheritance, but it is more lightweight than composition.
 */
abstract class UnsListeners<out T> : Property<T> {

    private val thread = Thread.currentThread()

    final override val mayChange: Boolean
        get() {
            checkThread()
            return true
        }

    final override val isConcurrent: Boolean
        get() {
            checkThread()
            return false
        }

    protected var listeners: Any? = null
    @Suppress("UNCHECKED_CAST")
    final override fun addChangeListener(onChange: ChangeListener<T>) {
        checkThread()
        val liss = listeners
        listeners = when {
            liss === null -> onChange
            liss is Function2<*, *, *> -> ArrayList<ChangeListener<T>>(2).also { it.add(liss as ChangeListener<T>); it.add(onChange) }
            liss.javaClass === ArrayList::class.java -> (liss as ArrayList<ChangeListener<T>>).apply { add(onChange) }
            else -> throw AssertionError()
        }
    }

    @Suppress("UNCHECKED_CAST")
    final override fun removeChangeListener(onChange: ChangeListener<T>) {
        checkThread()
        val liss = listeners
        listeners = when {
            liss === null -> null
            liss === onChange -> null
            liss is Function2<*, *, *> -> liss
            liss.javaClass === ArrayList::class.java -> (liss as ArrayList<ChangeListener<T>>).apply { remove(onChange) }
            else -> throw AssertionError()
        }
    }

    protected fun checkThread() {
        if (Thread.currentThread() !== thread)
            throw RuntimeException("${Thread.currentThread()} is not allowed to touch this property since it was created in $thread.")
    }

}
