package net.aquadc.properties.internal

/**
 * Represents state of concurrent property — its listeners and queue of values.
 * @property listeners to avoid breaking iteration loop,
 *                     removed listeners becoming nulls while iterating
 * @property pendingValues a list of updates to deliver. If not empty,
 *                         then notification is happening right now
 */
@Suppress("UNCHECKED_CAST")
internal class ConcListeners<out L : Any, out T>(
        @JvmField val listeners: Array<out L?>,
        @JvmField val pendingValues: Array<out T>,
        @JvmField val transitioningObservedState: Boolean,
        @JvmField val nextObservedState: Boolean
) {

    fun withListener(newListener: @UnsafeVariance L): ConcListeners<L, T> =
            ConcListeners(listeners.with(newListener) as Array<L?>, pendingValues, transitioningObservedState, nextObservedState)

    fun withoutListenerAt(idx: Int): ConcListeners<L, T> {
        val newListeners = when {
            pendingValues.isNotEmpty() ->
                (listeners as Array<L?>).clone().also { it[idx] = null }
            // we can't just remove this element while array is being iterated, nulling it out instead

            listeners.size == 1 ->
                EmptyArray as Array<L?>
            // our victim was the only listener, not notifying — returning a shared const

            else ->
                listeners.copyOfWithout(idx, EmptyArray) as Array<L?>
            // we're not the only listener, not notifying, remove at the specified position
        }

        return if (pendingValues.isEmpty() && newListeners.isEmpty() && !transitioningObservedState && !nextObservedState)
            NoListeners
        else
            ConcListeners(newListeners, pendingValues, transitioningObservedState, nextObservedState)
    }

    fun withNextValue(newValue: @UnsafeVariance T): ConcListeners<L, T> =
            ConcListeners(listeners, pendingValues.with(newValue) as Array<out T>, transitioningObservedState, nextObservedState)

    fun next(): ConcListeners<L, T> {
        val listeners = if (this.pendingValues.size == 1) {
            // 1 means we're stopping notification, will remove nulls then
            (this.listeners as Array<L?>).withoutNulls(EmptyArray as Array<L>)
        } else {
            this.listeners
        }

        // remove value at 0, that listeners were just notified about
        return ConcListeners(listeners, pendingValues.copyOfWithout(0, EmptyArray) as Array<out T>, transitioningObservedState, nextObservedState)
    }

    fun startTransition(): ConcListeners<L, T> {
        return ConcListeners(listeners, pendingValues, true, !nextObservedState)
    }

    fun continueTransition(appliedState: Boolean): ConcListeners<L, T> {
        check(transitioningObservedState)
        val done = appliedState == nextObservedState
        return ConcListeners(listeners, pendingValues, !done, nextObservedState)
    }


    internal companion object {
        @JvmField val EmptyArray =
                emptyArray<Any?>()

        @JvmField val NoListeners =
                ConcListeners(EmptyArray, EmptyArray, false, false) as ConcListeners<Nothing, Nothing>
    }

}
