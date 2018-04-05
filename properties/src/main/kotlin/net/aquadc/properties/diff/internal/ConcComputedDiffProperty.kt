package net.aquadc.properties.diff.internal

import net.aquadc.properties.Property
import net.aquadc.properties.executor.Worker


class ConcComputedDiffProperty<T, D>(
        original: Property<T>,
        calculateDiff: (T, T) -> D,
        computeOn: Worker
) : ConcDiffPropNotifier<T, D>() {

    init {
        /*
         * Oh... I don't want to have both single-threaded and concurrent implementations of Diff properties.
         * But a concurrent property aggregating a single-threaded property is, um... strange and unpredictable.
         */

        original.addChangeListener { old, new ->
            computeOn.map2(old, new, calculateDiff) {
                value = new
                valueChanged(old, new, it)
            }
        }
    }

    @Volatile
    override var value: T = original.value
        internal set

}
