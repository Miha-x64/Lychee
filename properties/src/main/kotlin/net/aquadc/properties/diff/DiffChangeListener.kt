package net.aquadc.properties.diff

/**
 * Observer for [DiffProperty]<T, D>.
 */
typealias DiffChangeListener<T, D> = (old: T, new: T, diff:D) -> Unit
