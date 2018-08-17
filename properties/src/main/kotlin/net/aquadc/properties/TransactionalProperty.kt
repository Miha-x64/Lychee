package net.aquadc.properties

/**
 * Represents a property value of which can be set only inside a transaction.
 */
interface TransactionalProperty<TRANSACTION, T> : Property<T> {

    fun setValue(transaction: TRANSACTION, value: T)

}
