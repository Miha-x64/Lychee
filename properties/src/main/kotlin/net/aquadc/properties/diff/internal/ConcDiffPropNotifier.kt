package net.aquadc.properties.diff.internal

import net.aquadc.properties.ChangeListener
import net.aquadc.properties.diff.DiffChangeListener
import net.aquadc.properties.diff.DiffProperty
import net.aquadc.properties.internal.PropListeners
import net.aquadc.properties.internal.update

/**
 * Despite the class is public, it is a part of private API.
 */
abstract class ConcDiffPropNotifier<T, D> : PropListeners<T, D, Any, Pair<T, D>>(null), DiffProperty<T, D> {

    final override fun addChangeListener(onChange: ChangeListener<T>) =
            concStateUpdater().update(this) { it.withListener(onChange) }

    final override fun addChangeListener(onChangeWithDiff: DiffChangeListener<T, D>) =
            concStateUpdater().update(this) { it.withListener(onChangeWithDiff) }

    final override fun removeChangeListener(onChange: ChangeListener<T>) =
            concStateUpdater().update(this) { it.withoutListener(onChange) }

    final override fun removeChangeListener(onChangeWithDiff: DiffChangeListener<T, D>) =
            concStateUpdater().update(this) { it.withoutListener(onChangeWithDiff) }

    final override fun pack(new: T, diff: D): Pair<T, D> =
            new to diff

    final override fun unpackValue(packed: Pair<T, D>): T =
            packed.first

    final override fun unpackDiff(packed: Pair<T, D>): D =
            packed.second

    @Suppress("UNCHECKED_CAST") // oh, so many of them
    final override fun notify(listener: Any, old: T, new: T, diff: D) = when (listener) {
        is Function2<*, *, *> -> (listener as ChangeListener<T>)(old, new)
        is Function3<*, *, *, *> -> (listener as DiffChangeListener<T, D>)(old, new, diff)
        else -> throw AssertionError()
    }

}
