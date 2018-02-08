package net.aquadc.properties.persistence

/**
 * Describes an object which holds data from properties.
 */
interface PropertiesMemento {

    /**
     * Pushes encapsulated data into [target].
     */
    fun restoreTo(target: PersistableProperties)

}
