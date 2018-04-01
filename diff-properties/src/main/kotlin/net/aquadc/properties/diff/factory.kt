package net.aquadc.properties.diff

import net.aquadc.properties.diff.internal.ConcMutableDiffProperty


fun <T, D> concurrentMutableDiffPropertyOf(value: T): MutableDiffProperty<T, D> =
        ConcMutableDiffProperty(value)
