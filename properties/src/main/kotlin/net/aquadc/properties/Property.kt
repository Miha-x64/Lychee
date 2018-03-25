package net.aquadc.properties

/**
 * Represents an observable value.
 */
interface Property<out T> {

    /**
     * Returns current value.
     */
    fun getValue(): T

    /**
     * When `true`, means that property value may change in future.
     */
    val mayChange: Boolean

    /**
     * When `true`, means that property value can be
     * mutated and observed from different threads.
     */
    val isConcurrent: Boolean

    /**
     * Subscribe on value changes.
     */
    fun addChangeListener(onChange: ChangeListener<T>)

    /**
     * Unsubscribe from value changes.
     * It [onChange] is not registered as a listener, does nothing.
     */
    fun removeChangeListener(onChange: ChangeListener<T>)

}
