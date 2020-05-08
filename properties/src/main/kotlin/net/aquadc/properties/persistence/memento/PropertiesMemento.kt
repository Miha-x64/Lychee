@file:JvmName("PropertiesState")
package net.aquadc.properties.persistence.memento

import net.aquadc.properties.persistence.PropertyIo

/**
 * Describes an object which holds data from properties.
 */
interface PropertiesMemento {

    /**
     * Creates a reader over encapsulated data.
     */
    fun reader(): PropertyIo

}

/**
 * Pushes data from [this] into [target].
 */
fun PropertiesMemento.restoreTo(target: PersistableProperties): Unit =
        target.saveOrRestore(reader())
