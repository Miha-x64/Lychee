package net.aquadc.properties.diff.internal

import net.aquadc.properties.ChangeListener
import net.aquadc.properties.Property
import net.aquadc.properties.addUnconfinedChangeListener
import net.aquadc.properties.executor.Worker


@PublishedApi
internal class `ConcComputedDiff-`<T, D, F : Any>(
        private val original: Property<T>,
        private val calculateDiff: (T, T) -> D,
        private val computeOn: Worker<F>
) : `ConcDiff-Notifier`<T, D>(), ChangeListener<T>, (T, T, D) -> Unit {

    /*
     * Oh... I don't want to have both single-threaded and concurrent implementations of Diff properties.
     * But a concurrent property aggregating a single-threaded property is, um... strange and unpredictable.
     */

    @Volatile
    override var value: T = original.value
        private set

    // we don't need atomic.getAndSet here because listeners are called strictly sequentially.
    // I'm even not sure whether volatile useful here
    @Volatile private var running: F? = null

    override fun invoke(old: T, new: T) {
        running?.let(computeOn::cancel)
        running = computeOn.map2(old, new, calculateDiff, this)
    }

    override fun invoke(old: T, new: T, diff: D) {
        value = new
        valueChanged(old, new, diff)
    }

    override fun observedStateChanged(observed: Boolean) {
        if (observed) {
            original.addUnconfinedChangeListener(this)
        } else {
            original.removeChangeListener(this)
            running?.let {
                computeOn.cancel(it)
                running = null
            }
        }
    }

}
