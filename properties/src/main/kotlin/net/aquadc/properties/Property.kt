package net.aquadc.properties

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
     * Subscribe on value changes.
     * Acts like [MutableList.add]: [onChange] will be added in any case.
     */
    fun addChangeListener(onChange: ChangeListener<T>)

    /**
     * Unsubscribe from value changes.
     * Acts like [MutableList.remove]: removes only first occurrence of [onChange], if any.
     * But checks equality by identity, not by [equals].
     */
    fun removeChangeListener(onChange: ChangeListener<T>)

}
