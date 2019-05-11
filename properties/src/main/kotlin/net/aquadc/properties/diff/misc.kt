@file:Suppress("NOTHING_TO_INLINE")
@file:JvmName("DiffProperties")
package net.aquadc.properties.diff

import net.aquadc.properties.Property
import net.aquadc.properties.diff.internal.`ConcComputedDiff-`
import net.aquadc.properties.diff.internal.`ConcMutableDiff-`
import net.aquadc.properties.executor.UnconfinedExecutor
import net.aquadc.properties.executor.Worker


/**
 * Observer for [DiffProperty]<T, D>.
 */
typealias DiffChangeListener<T, D> = (old: T, new: T, diff:D) -> Unit

/**
 * Returns new multi-threaded [MutableDiffProperty] with initial value [value].
 */
inline fun <T, D> concurrentDiffPropertyOf(value: T): MutableDiffProperty<T, D> =
        `ConcMutableDiff-`(value)

/**
 * Returns new [DiffProperty] calculated from [this] [Property].
 */
inline fun <T, D, F : Any> Property<T>.calculateDiffOn(worker: Worker<F>, noinline calculate: (T, T) -> D): DiffProperty<T, D> =
        `ConcComputedDiff-`(this, calculate, worker)

/**
 * Observes this property on [UnconfinedExecutor], i. e. on whatever thread.
 */
inline fun <T, D> DiffProperty<T, D>.addUnconfinedChangeListener(noinline onChange: DiffChangeListener<T, D>) {
    addChangeListenerOn(UnconfinedExecutor, onChange)
}
