package net.aquadc.properties.diff

import net.aquadc.properties.Property
import net.aquadc.properties.diff.internal.ConcComputedDiffProperty
import net.aquadc.properties.executor.Worker

fun <T, D> Property<T>.calculateDiffOn(worker: Worker, calculate: (T, T) -> D): DiffProperty<T, D> =
        ConcComputedDiffProperty(this, calculate, worker)
