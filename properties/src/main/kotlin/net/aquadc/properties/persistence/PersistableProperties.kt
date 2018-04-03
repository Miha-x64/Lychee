package net.aquadc.properties.persistence

/**
 * Describes a state-representing class. Instances of it can be saved and restored.
 * Similar to [java.io.Externalizable] or `android.os.Parcelable`,
 * but instead of declaring separate read and write methods
 * which are mirroring each other you just declare a single method.
 */
interface PersistableProperties {

    /**
     * Gives properties to input or output.
     * Should consist of [x] method calls.
     * State will be either read or written depending on type of [d].
     */
    fun saveOrRestore(d: PropertyIo)

}
