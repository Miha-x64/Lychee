package net.aquadc.properties

/**
 * Represents an observable value of type [T].
 */
interface Property<out T> {

    /**
     * Current value.
     */
    val value: T

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
     * If [onChange] is not registered as a listener, does nothing.
     */
    fun removeChangeListener(onChange: ChangeListener<T>)

}
