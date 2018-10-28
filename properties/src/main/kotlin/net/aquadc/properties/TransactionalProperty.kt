package net.aquadc.properties

/**
 * Represents a property value of which can be set only inside a transaction.
 */
interface TransactionalProperty<TRANSACTION, T> : Property<T> {

    /**
     * Updates value of this property to [value].
     * This may alter 'dirty' state making changes visible only to current thread.
     * Notification will be triggered only when transaction is successful.
     * @throws IllegalStateException if not managed anymore or [transaction] is already closed
     */
    fun setValue(transaction: TRANSACTION, value: T)

}
