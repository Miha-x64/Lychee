package net.aquadc.properties.diff.internal

import net.aquadc.properties.ChangeListener
import net.aquadc.properties.Property
import net.aquadc.properties.addUnconfinedChangeListener
import net.aquadc.properties.executor.Worker


@PublishedApi
internal class ConcComputedDiffProperty<T, D>(
        private val original: Property<T>,
        private val calculateDiff: (T, T) -> D,
        private val computeOn: Worker
) : ConcDiffPropNotifier<T, D>(), ChangeListener<T> {

    /*
     * Oh... I don't want to have both single-threaded and concurrent implementations of Diff properties.
     * But a concurrent property aggregating a single-threaded property is, um... strange and unpredictable.
     */

    @Volatile
    override var value: T = this as T
        get() {
            val v = field
            return if (v === this) original.value else v
        }
        private set

    override fun invoke(old: T, new: T) {
        computeOn.map2(old, new, calculateDiff) {
            valueChangedAccess(old, new, it)
        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    internal fun valueChangedAccess(old: T, new: T, diff: D) {
        value = new
        valueChanged(old, new, diff)
    }

    override fun observedStateChanged(observed: Boolean) {
        if (observed) {
            original.addUnconfinedChangeListener(this)
        } else {
            original.removeChangeListener(this)
        }
    }

}
