package net.aquadc.properties

/**
 * Value of such property may ve changed 'by hand' or bound to other property's value.
 */
interface MutableProperty<T> : Property<T>/*, TransactionalProperty<Nothing?, T>*/ {
    // this could be done only by changing `Bound-` source code     ^^^^^^^^

    /**
     * Current value.
     * Setter changes it and notifies listeners. Will break current binding, if any.
     */
    override var value: T

    /**
     * Bind this property's value to [sample]'s value.
     */
    fun bindTo(sample: Property<T>)

    /**
     * CompareAndSet value atomically. Will break binding, if any.
     */
    fun casValue(expect: T, update: T): Boolean

    /*override fun setValue(transaction: Nothing?, value: T) {
        this.value = value
    }*/

}
