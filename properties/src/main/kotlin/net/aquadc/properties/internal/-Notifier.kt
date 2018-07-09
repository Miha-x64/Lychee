package net.aquadc.properties.internal

import net.aquadc.properties.ChangeListener
import net.aquadc.properties.executor.ConfinedChangeListener
import net.aquadc.properties.executor.PlatformExecutors
import net.aquadc.properties.executor.UnconfinedExecutor
import java.util.concurrent.Executor


/**
 * Base class containing concurrent notification logic.
 * Despite class is public, this is private API.
 */
abstract class `-Notifier`<out T>(thread: Thread?) :
        `-Listeners`<T, Nothing?, ChangeListener<@UnsafeVariance T>, @UnsafeVariance T>(thread) {

    protected fun isBeingObserved(): Boolean =
            if (thread == null) {
                concStateUpdater().get(this).listeners.any { it != null }
            } else {
                val lis = nonSyncListeners
                when (lis) {
                    null -> false
                    is Function2<*, *, *> -> true
                    is Array<*> -> lis.any { it != null }
                    else -> throw AssertionError()
                }
            }

    final override fun addChangeListener(onChange: ChangeListener<T>) {
        if (thread == null) {
            concAddChangeListenerInternal(
                    ConfinedChangeListener(PlatformExecutors.executorForCurrentThread(), onChange)
            )
        } else {
            nonSyncAddChangeListenerInternal(
                    onChange // no explicit Executor, will be notified on current thread
            )
        }
    }

    final override fun addChangeListenerOn(executor: Executor, onChange: ChangeListener<T>) {
        if (thread == null) {
            concAddChangeListenerInternal(
                    if (executor === UnconfinedExecutor) onChange else ConfinedChangeListener(executor, onChange)
            )
        } else {
            nonSyncAddChangeListenerInternal(
                    if (executor === UnconfinedExecutor || executor === PlatformExecutors.executors.get()) onChange
                    else ConfinedChangeListener(executor, onChange)
            )
        }
    }

    /**
     * Note: this will remove first occurrence of [onChange],
     * no matter on which executor it was subscribed.
     */
    final override fun removeChangeListener(onChange: ChangeListener<T>) {
        removeChangeListenerWhere { listener ->
            when {
                listener === onChange ->
                    true

                listener is ConfinedChangeListener<*> && listener.actual === onChange ->
                    true.also { listener.canceled = true }

                else ->
                    false
            }
        }
    }

    final override fun pack(new: @UnsafeVariance T, diff: Nothing?): T =
            new

    final override fun unpackValue(packed: @UnsafeVariance T): T =
            packed

    final override fun unpackDiff(packed: @UnsafeVariance T): Nothing? =
            null

    final override fun notify(listener: ChangeListener<T>, old: @UnsafeVariance T, new: @UnsafeVariance T, diff: Nothing?): Unit =
            listener(old, new)

}
