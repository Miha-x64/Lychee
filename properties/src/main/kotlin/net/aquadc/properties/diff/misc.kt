@file:Suppress("NOTHING_TO_INLINE")
@file:JvmName("DiffProperties")
package net.aquadc.properties.diff

import net.aquadc.properties.Property
import net.aquadc.properties.diff.internal.ConcComputedDiffProperty
import net.aquadc.properties.diff.internal.ConcMutableDiffProperty
import net.aquadc.properties.executor.Worker


/**
 * Observer for [DiffProperty]<T, D>.
 */
typealias DiffChangeListener<T, D> = (old: T, new: T, diff:D) -> Unit

inline fun <T, D> concurrentMutableDiffPropertyOf(value: T): MutableDiffProperty<T, D> =
        ConcMutableDiffProperty(value)

inline fun <T, D> Property<T>.calculateDiffOn(worker: Worker, noinline calculate: (T, T) -> D): DiffProperty<T, D> =
        ConcComputedDiffProperty(this, calculate, worker)
