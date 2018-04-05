@file:Suppress("NOTHING_TO_INLINE")

package net.aquadc.properties.diff

import net.aquadc.properties.diff.internal.ConcMutableDiffProperty


inline fun <T, D> concurrentMutableDiffPropertyOf(value: T): MutableDiffProperty<T, D> =
        ConcMutableDiffProperty(value)
