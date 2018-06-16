@file:Suppress("NOTHING_TO_INLINE")
@file:JvmName("DiffProperties")
package net.aquadc.properties.diff

import net.aquadc.properties.Property
import net.aquadc.properties.diff.internal.ConcComputedDiffProperty
import net.aquadc.properties.diff.internal.ConcMutableDiffProperty
import net.aquadc.properties.executor.UnconfinedExecutor
import net.aquadc.properties.executor.Worker


/**
 * Observer for [DiffProperty]<T, D>.
 */
typealias DiffChangeListener<T, D> = (old: T, new: T, diff:D) -> Unit

/**
 * Returns new multi-threaded [MutableDiffProperty] with initial value [value].
 */
inline fun <T, D> concurrentMutableDiffPropertyOf(value: T): MutableDiffProperty<T, D> =
        ConcMutableDiffProperty(value)

/**
 * Returns new [DiffProperty] calculated from [this] [Property].
 */
inline fun <T, D> Property<T>.calculateDiffOn(worker: Worker, noinline calculate: (T, T) -> D): DiffProperty<T, D> =
        ConcComputedDiffProperty(this, calculate, worker)

/**
 * Observes this property on [UnconfinedExecutor], i. e. on whatever thread.
 */
inline fun <T, D> DiffProperty<T, D>.addUnconfinedChangeListener(noinline onChange: DiffChangeListener<T, D>) {
    addChangeListenerOn(UnconfinedExecutor, onChange)
}
