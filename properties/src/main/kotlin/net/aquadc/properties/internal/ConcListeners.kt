package net.aquadc.properties.internal

private const val TransitioningObservedState = 1
private const val NextObservedState = 2
private const val TransitionLocked = 4

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
        @JvmField val flags: Int // replacing 2+ booleans with a single int may potentially be more compact on ARMs
) {

    inline val transitioningObservedState: Boolean get() = (flags and TransitioningObservedState) != 0
    inline val nextObservedState: Boolean get() = (flags and NextObservedState) != 0
    inline val transitionLocked: Boolean get() = (flags and TransitionLocked) != 0

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
        return ConcListeners(listeners, pendingValues, flags)
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
            ConcListeners(newListeners, pending, flags)
    }

    fun withNextValue(newValue: @UnsafeVariance T): ConcListeners<L, T> =
            ConcListeners(listeners, pending.with(newValue), flags)

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
        return ConcListeners(listeners, pending.copyOfWithout(0, EmptyArray), flags)
    }

    fun startTransition(): ConcListeners<L, T> =
            ConcListeners(listeners, pending, flags or TransitioningObservedState xor NextObservedState)

    fun continueTransition(appliedState: Boolean): ConcListeners<L, T> {
        check(transitioningObservedState)
        val done = appliedState == nextObservedState
        val flags = if (done) flags and TransitioningObservedState.inv() else flags or TransitioningObservedState
        return ConcListeners(listeners, pending, flags)
    }

    fun flippedTransitionLock(): ConcListeners<L, T> =
            ConcListeners(listeners, pending, flags xor TransitionLocked)

    fun withoutPendingAt(idx: Int): ConcListeners<L, T> {
        check(pending[idx] is AddListener<*>)
        return ConcListeners(listeners, pending.copyOfWithout(idx, EmptyArray), flags)
    }

    internal class AddListener<L>(@JvmField @JvmSynthetic internal val listener: L)

}
