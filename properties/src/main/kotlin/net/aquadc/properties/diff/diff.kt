package net.aquadc.properties.diff

import net.aquadc.properties.Property
import net.aquadc.properties.diff.internal.ConcComputedDiffProperty
import net.aquadc.properties.executor.Worker

@Suppress("NOTHING_TO_INLINE")
inline fun <T, D> Property<T>.calculateDiffOn(worker: Worker, noinline calculate: (T, T) -> D): DiffProperty<T, D> =
        ConcComputedDiffProperty(this, calculate, worker)
