package net.aquadc.properties.diff

/**
 * A [DiffProperty] which can be mutated 'by hand'.
 */
interface MutableDiffProperty<T, D> : DiffProperty<T, D> {

    /**
     * CAS [expectValue] with [newValue] which are different by [diff].
     */
    fun casValue(expectValue: T, newValue: T, diff: D): Boolean

}
