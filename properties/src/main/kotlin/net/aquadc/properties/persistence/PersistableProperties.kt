package net.aquadc.properties.persistence

/**
 * Describes a state-class which can be saved and restored.
 */
interface PersistableProperties {

    /**
     * Gives properties to (de)serializer.
     * Should consist of [x] method calls.
     */
    fun saveOrRestore(d: PropertyIo)

}
