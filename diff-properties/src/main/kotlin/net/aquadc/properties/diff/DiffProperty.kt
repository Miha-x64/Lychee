package net.aquadc.properties.diff

import net.aquadc.properties.Property

/**
 * Represents a property which provides a diff along with value changes.
 */
interface DiffProperty<out T, out D> : Property<T> {

    /**
     * Subscribe on value changes.
     */
    fun addChangeListener(onChangeWithDiff: DiffChangeListener<T, D>)

    /**
     * Unsubscribe from value changes.
     * If [onChangeWithDiff] is not registered as a listener, does nothing.
     */
    fun removeChangeListener(onChangeWithDiff: DiffChangeListener<T, D>)

}
