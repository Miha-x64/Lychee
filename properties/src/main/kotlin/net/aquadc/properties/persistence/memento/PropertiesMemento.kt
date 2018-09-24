package net.aquadc.properties.persistence.memento

/**
 * Describes an object which holds data from properties.
 */
interface PropertiesMemento {

    /**
     * Pushes encapsulated data into [target].
     */
    fun restoreTo(target: PersistableProperties)

}
