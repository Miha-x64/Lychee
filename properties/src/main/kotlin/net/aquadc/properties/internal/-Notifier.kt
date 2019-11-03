package net.aquadc.properties.internal

import androidx.annotation.RestrictTo
import net.aquadc.properties.ChangeListener
import net.aquadc.properties.executor.ConfinedChangeListener
import net.aquadc.properties.executor.PlatformExecutors
import net.aquadc.properties.executor.UnconfinedExecutor
import java.util.concurrent.Executor


/**
 * Base class containing concurrent notification logic.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
abstract class `-Notifier`<out T>(
        concurrent: Boolean
) : `-Listeners`<T, Nothing?, ChangeListener<@UnsafeVariance T>, @UnsafeVariance T>(
        if (concurrent) null else Thread.currentThread()
) {

    protected fun isBeingObserved(): Boolean =
            if (thread == null) {
                concState().get().listeners.any { it != null }
            } else {
                when (val lis = nonSyncListeners) {
                    null -> false
                    is Function2<*, *, *> -> true
                    is Array<*> -> lis.any { it != null }
                    else -> throw AssertionError()
                }
            }

    @Suppress("USELESS_IS_CHECK") // I know you're lying for multi-arity functions
    final override fun addChangeListener(onChange: ChangeListener<T>) {
        if (thread == null) {
            concAddChangeListenerInternal(
                    ConfinedChangeListener<T, Nothing?>(PlatformExecutors.requireCurrent(), onChange, null)
            )
        } else {
            nonSyncAddChangeListenerInternal(
                    if (onChange is Function2) onChange // no explicit Executor, will be notified on current thread
                    else ConfinedChangeListener<T, Nothing>(UnconfinedExecutor, onChange, null)
            )
        }
    }

    @Suppress("USELESS_IS_CHECK") // I know you're lying for multi-arity functions
    final override fun addChangeListenerOn(executor: Executor, onChange: ChangeListener<T>) {
        if (thread == null) {
            concAddChangeListenerInternal(
                    if (executor === UnconfinedExecutor) onChange
                    else ConfinedChangeListener<T, Nothing?>(executor, onChange, null)
            )
        } else {
            nonSyncAddChangeListenerInternal(
                    if ((executor === UnconfinedExecutor || executor === PlatformExecutors.getCurrent()) && onChange is Function2<*, *, *>) onChange
                    else ConfinedChangeListener<T, Nothing?>(executor, onChange, null)
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

                listener is ConfinedChangeListener<*, *> && listener.actual === onChange ->
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
