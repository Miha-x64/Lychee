package net.aquadc.properties.internal

/**
 * Represents state of concurrent property — its listeners and queue of values.
 * @property listeners to avoid breaking iteration loop,
 *                     removed listeners becoming nulls while iterating
 * @property pending    a list of updates to deliver, consisting of next values and listeners.
 *                      If not empty, then notification is happening right now
 */
@Suppress("UNCHECKED_CAST")
internal class ConcListeners<out L : Any, out T>(
        @JvmField val listeners: Array<out L?>,
        @JvmField val pending: Array<Any?>, // = out T | AddListener<L>
        @JvmField val transitioningObservedState: Boolean,
        @JvmField val nextObservedState: Boolean,
        @JvmField val transitionLocked: Boolean
) {

    fun withListener(newListener: @UnsafeVariance L): ConcListeners<L, T> {
        var listeners = listeners
        var pendingValues = pending
        if (pendingValues.isEmpty()) {
            // currently not notifying — can just add
            listeners = listeners.with(newListener) as Array<L?>
        } else {
            // currently notifying — must postpone adding listener until the moment when current value delivered
            pendingValues = pendingValues.with(AddListener(newListener))
        }
        return ConcListeners(listeners, pendingValues, transitioningObservedState, nextObservedState, transitionLocked)
    }

    fun withoutListenerAt(idx: Int): ConcListeners<L, T> {
        val newListeners = when {
            pending.isNotEmpty() ->
                (listeners as Array<L?>).clone().also { it[idx] = null }
            // we can't just remove this element while array is being iterated, nulling it out instead

            listeners.size == 1 ->
                (EmptyArray as Array<L?>).also { check(idx == 0) }
            // our victim was the only listener, not notifying — returning a shared const

            else ->
                listeners.copyOfWithout(idx, EmptyArray) as Array<L?>
            // we're not the only listener, not notifying, remove at the specified position
        }

        return if (pending.isEmpty() && newListeners.isEmpty() && !transitioningObservedState && !nextObservedState)
            NoListeners
        else
            ConcListeners(newListeners, pending, transitioningObservedState, nextObservedState, transitionLocked)
    }

    fun withNextValue(newValue: @UnsafeVariance T): ConcListeners<L, T> =
            ConcListeners(listeners, pending.with(newValue), transitioningObservedState, nextObservedState, transitionLocked)

    fun next(): ConcListeners<L, T> {
        var listeners = if (this.pending.size == 1) {
            // 1 means we're stopping notification, will remove nulls then
            (this.listeners as Array<L?>).withoutNulls(EmptyArray as Array<L>)
        } else {
            this.listeners
        }

        // now it's time to add a listener which was added during notification
        (pending[0] as? AddListener<L>)?.let { pendingListener ->
            listeners = listeners.with(pendingListener.listener) as Array<out L?>
        }

        // remove value at 0, that listeners were just notified about
        return ConcListeners(listeners, pending.copyOfWithout(0, EmptyArray),
                transitioningObservedState, nextObservedState, transitionLocked)
    }

    fun startTransition(): ConcListeners<L, T> =
            ConcListeners(listeners, pending, true, !nextObservedState, transitionLocked)

    fun continueTransition(appliedState: Boolean): ConcListeners<L, T> {
        check(transitioningObservedState)
        val done = appliedState == nextObservedState
        return ConcListeners(listeners, pending, !done, nextObservedState, transitionLocked)
    }

    fun flippedTransitionLock(): ConcListeners<L, T> =
            ConcListeners(listeners, pending, transitioningObservedState, nextObservedState, !transitionLocked)

    fun withoutPendingAt(idx: Int): ConcListeners<L, T> {
        check(pending[idx] is AddListener<*>)
        return ConcListeners(listeners, pending.copyOfWithout(idx, EmptyArray), transitioningObservedState, nextObservedState, transitionLocked)
    }

    internal class AddListener<L>(@JvmField @JvmSynthetic internal val listener: L)

}
