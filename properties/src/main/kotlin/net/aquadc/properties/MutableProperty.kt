package net.aquadc.properties

/**
 * Value of such property may ve changed 'by hand' or bound to other property's value.
 */
interface MutableProperty<T> : Property<T> {

    /**
     * Changes value and notifies listeners. Will break current binding, if any.
     */
    fun setValue(newValue: T)

    /**
     * Bind this property's value to [sample]'s value.
     */
    fun bindTo(sample: Property<T>)

    /**
     * CompareAndSet value atomically. Will break binding, if any.
     */
    fun cas(expect: T, update: T): Boolean

}
