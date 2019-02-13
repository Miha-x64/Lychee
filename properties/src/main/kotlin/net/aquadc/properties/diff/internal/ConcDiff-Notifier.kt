package net.aquadc.properties.diff.internal

import net.aquadc.properties.ChangeListener
import net.aquadc.properties.diff.DiffChangeListener
import net.aquadc.properties.diff.DiffProperty
import net.aquadc.properties.executor.ConfinedChangeListener
import net.aquadc.properties.executor.PlatformExecutors
import net.aquadc.properties.executor.UnconfinedExecutor
import net.aquadc.properties.internal.`-Listeners`
import java.util.concurrent.Executor


internal abstract class `ConcDiff-Notifier`<T, D> : `-Listeners`<T, D, Function<Unit>, Pair<T, D>>(null), DiffProperty<T, D> {

    final override fun addChangeListener(onChange: ChangeListener<T>) =
            concAddChangeListenerInternal(
                    ConfinedChangeListener<T, Nothing?>(PlatformExecutors.executorForCurrentThread(), onChange, null)
            )

    @Suppress("USELESS_IS_CHECK") // I know you're lying for multi-arity listeners
    override fun addChangeListenerOn(executor: Executor, onChange: ChangeListener<T>) =
            concAddChangeListenerInternal(
                    if (executor === UnconfinedExecutor && onChange is Function2<*, *, *>) onChange
                    else ConfinedChangeListener<T, Nothing?>(executor, onChange, null)
            )

    final override fun removeChangeListener(onChange: ChangeListener<T>) =
            rm(onChange)


    final override fun addChangeListener(onChangeWithDiff: DiffChangeListener<T, D>) =
            concAddChangeListenerInternal(
                    ConfinedChangeListener(PlatformExecutors.executorForCurrentThread(), null, onChangeWithDiff)
            )

    @Suppress("USELESS_IS_CHECK") // I know you're lying for multi-arity listeners
    override fun addChangeListenerOn(executor: Executor, onChangeWithDiff: DiffChangeListener<T, D>) =
            concAddChangeListenerInternal(
                    if (executor === UnconfinedExecutor && onChangeWithDiff is Function3<*, *, *, *>) onChangeWithDiff
                    else ConfinedChangeListener(executor, null, onChangeWithDiff)
            )

    final override fun removeChangeListener(onChangeWithDiff: DiffChangeListener<T, D>) =
            rm(onChangeWithDiff)


    final override fun pack(new: T, diff: D): Pair<T, D> =
            new to diff

    final override fun unpackValue(packed: Pair<T, D>): T =
            packed.first

    final override fun unpackDiff(packed: Pair<T, D>): D =
            packed.second

    private fun rm(onChange: Function<Unit>) {
        removeChangeListenerWhere { listener ->
            when {
                listener === onChange ->
                    true

                listener is ConfinedChangeListener<*, *> && listener.actual === onChange ->
                    true.also { listener.canceled = true }

                else ->
                    false
            }
        }
    }

    @Suppress("UNCHECKED_CAST") // oh, so many of them
    final override fun notify(listener: Function<Unit>, old: T, new: T, diff: D) = when (listener) {
        // we could just prefer ternary, if there was no Kotlin bug with multi-arity
        is ConfinedChangeListener<*, *> -> (listener as ConfinedChangeListener<T, D>)(old, new, diff)
        is Function2<*, *, *> -> (listener as ChangeListener<T>)(old, new)
        is Function3<*, *, *, *> -> (listener as DiffChangeListener<T, D>)(old, new, diff)
        else -> throw AssertionError()
    }

}
