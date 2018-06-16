package net.aquadc.properties

import java.util.concurrent.Executor

/**
 * Represents an observable value of type [T].
 * If not concurrent, *every* method will throw [RuntimeException],
 * including [isConcurrent] property getter.
 */
interface Property<out T> {

    /**
     * Current value.
     */
    val value: T

    /**
     * When `true`, means that property value may change in future.
     * This is `false` on immutable properties.
     */
    val mayChange: Boolean

    /**
     * When `true`, this property can be touched from
     * threads which are different from [Thread.currentThread].
     * If `false`, this property is fully bound to current thread.
     */
    val isConcurrent: Boolean

    /**
     * Subscribe on value changes on current thread.
     * Acts like [MutableList.add]: [onChange] will be added even if it is already added.
     * @throws UnsupportedOperationException if this thread's executor cannot be identified
     */
    fun addChangeListener(onChange: ChangeListener<T>)

    /**
     * Subscribe on value changes on [executor].
     * Acts like [MutableList.add]: [onChange] will be added even if it is already added.
     */
    fun addChangeListenerOn(executor: Executor, onChange: ChangeListener<T>)

    /**
     * Unsubscribe from value changes.
     * Acts like [MutableList.remove]: removes only first occurrence of [onChange], if any.
     * But checks equality by identity, not with [equals].
     */
    fun removeChangeListener(onChange: ChangeListener<T>)

}
