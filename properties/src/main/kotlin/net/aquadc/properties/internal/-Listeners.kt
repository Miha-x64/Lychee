package net.aquadc.properties.internal

import androidx.annotation.RestrictTo
import net.aquadc.properties.Property
import java.util.concurrent.atomic.AtomicReference

// I don't like implementation inheritance, but it is more lightweight than composition.

/**
 * Base class containing concurrent props' listeners.
 * Used by [-Notifier] and [ConcMutableDiff-].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
abstract class `-Listeners`<out T, in D, LISTENER : Any, UPDATE> : AtomicReference<Any?>, Property<T> {

    /**
     * our thread, or null, if this property is concurrent
     */
    @JvmField internal val thread: Thread?

    /**
     * {@implNote
     *   this [AtomicReference] has type [ConcListeners]<LISTENER, UPDATE> for concurrent properties
     *   and `UPDATE[]?` for non-synchronized ones;
     *   single-thread properties store listeners separately, in [nonSyncListeners].
     * }
     */
    @Suppress("ConvertSecondaryConstructorToPrimary")
    internal constructor(thread: Thread?) : super(
            if (thread == null) NoListeners else null
    ) {
        this.thread = thread
    }

    final override val mayChange: Boolean
        get() {
            if (thread != null) checkThread()
            return true
        }

    final override val isConcurrent: Boolean
        get() {
            val concurrent = thread == null
            if (!concurrent) checkThread()
            return concurrent
        }

    /**
     * Holds listeners of a non-synchronized property. Unused, if this is a concurrent one.
     * When unused, this introduces no sadness:
     * on 64-bit HotSpot with compressed OOPS and 8-byte object alignment,
     * instances of this class have size of 24 bytes.
     * Without [nonSyncListeners] this space would be occupied by padding.
     */
    @JvmField @JvmSynthetic(/* hide */)
    internal var nonSyncListeners: Any? = null

    /**
     * It's a tricky one.
     *
     * First, while notifying, can't remove listeners from the list/array because this will break indices
     * which are being used for iteration; nulling out removed listeners instead.
     *
     * Second, can't update value and trigger notification while already notifying:
     * this will lead to stack overflow and/or sequentially-inconsistent notifications;
     * must persist all pending values and deliver them later.
     *
     * Third, while current value is up-to-date, notifications can be late.
     * Thus, listeners added during notification must be postponed until notification process reaches current value.
     */
    protected fun valueChanged(old: @UnsafeVariance T, new: @UnsafeVariance T, diff: D) {
        if (thread == null) concValueChanged(old, new, diff)
        else nonSyncValueChanged(old, new, diff)
    }

    private fun concValueChanged(old: @UnsafeVariance T, new: @UnsafeVariance T, diff: D) {
        val oldListeners = concEnqueueUpdate(old, new, diff)
                ?: return // nothing to notify

        if (oldListeners.pending.isNotEmpty())
            return // other [valueChanged] is on the stack or in parallel,
        // [new] was added to pending values and will be delivered soon

        /*
         * [pending] is not empty now, it's a kind of lock
         * new values were added to pending list, let's deliver all the pending values,
         * including those which will appear during the notification
         */

        var prev = old
        while (true) {
            val state = concState().get() as ConcListeners<LISTENER, UPDATE>

            val current = state.pending[0] // take a pending value from prev state

            if (current !is ConcListeners.AddListener<*>) {
                current as UPDATE
                val currentValue = unpackValue(current)
                concNotifyAll(prev, currentValue, unpackDiff(current))
                prev = currentValue
            } // otherwise ConcListeners will handle listener itself

            // `pending[0]` is delivered, now remove it from our 'queue'

            /*
             * taking fresh state is safe because the only one thread/function is allowed to notify.
             * Just remove our 'queue' head, but let anyone add pending values or listeners
             */
            val next = concState()
                    .updateUndGet(ConcListeners<LISTENER, UPDATE>::next)

            if (next.pending.isEmpty()) {
                // all pending notified, nulled out listeners are removed
                return // success! go home.
            }
        }
    }

    /**
     * Enqueues [new] value. If no one notifies listeners right now, takes the right to do it.
     * @return previous state, or `null`, if there's nothing to do
     */
    private fun concEnqueueUpdate(old: @UnsafeVariance T, new: @UnsafeVariance T, diff: D): ConcListeners<LISTENER, UPDATE>? {
        var prev: ConcListeners<LISTENER, UPDATE>
        var next: ConcListeners<LISTENER, UPDATE>
        do {
            prev = concState().get()

            if (prev.listeners.isEmpty())
                return null // nothing to notify

            if (prev.pending.isNotEmpty()) {
                // other thread or stack performs notification at the moment

                /*
                 * if another thread made value CAS earlier,
                 * but have not made CAS on listeners yet, just wait
                 */

                val lastPendingIdx = prev.pending.indexOfLast { it !is ConcListeners.AddListener<*> }
                if (lastPendingIdx >= 0 && unpackValue(prev.pending[lastPendingIdx] as UPDATE) !== old) {
                    Thread.yield() // a bit of awful programming, yay!
                    // wait until other thread set its update, we can't help here
                    continue
                }
            }

            /*
             * at this point, either
             * prev.pending.isNotEmpty() && prev.pending.last().first == old
             * or
             * prev.pending.isEmpty()
             */

            next = prev.withNextValue(pack(new, diff))

            if (concState().compareAndSet(prev, next)) {
                return prev
            }

        } while (true)
    }

    private fun concNotifyAll(old: T, new: T, diff: D) {
        var i = 0
        var listeners = concState().get().listeners
        val size = listeners.size // listeners.size may change, but we don't want to touch those newly added ones
        while (i < size) {
            val listener = listeners[i]
            if (listener != null) {
                notify(listener, old, new, diff)
            }

            // read volatile listeners on every step, they may be nulled out!
            listeners = concState().get().listeners
            i++
        }
    }


    private fun nonSyncValueChanged(old: @UnsafeVariance T, new: @UnsafeVariance T, diff: D) {
        // if we have no one to notify, just give up
        if (nonSyncListeners == null) return

        nonSyncPending().get()?.let { pendingValues ->
            // This method is already on the stack, somewhere deeper.
            // Just push the new value...
            val newValues = pendingValues.with(pack(new, diff))

            @Suppress("UNCHECKED_CAST")
            nonSyncPending().lazySet(newValues)

            // and let deeper version of this method pick it up.
            return
        }

        @Suppress("UNCHECKED_CAST") // this means 'don't notify directly, add to the queue!'
        nonSyncPending().lazySet(EmptyArray)
        // pending is empty array now

        // now we own notification process

        nonSyncNotifyAll(old, new, diff)

        var pendingValues = nonSyncPending().get()!!
        var i = 0
        var older = new
        while (true) { // now take enqueued values set by notified properties
            if (i == pendingValues.size)
                break // real end of queue, nothing to do here

            val newer = pendingValues[i]
            if (newer is ConcListeners.AddListener<*>) {
                nonSyncReallyAddChangeListener(newer.listener as LISTENER, pendingValues)
            } else {
                newer as UPDATE
                val newerValue = unpackValue(newer)
                nonSyncNotifyAll(older, newerValue, unpackDiff(newer))
                older = newerValue
            }
            i++

            if (i == pendingValues.size) // end of queue, read a fresh one
                pendingValues = nonSyncPending().get()!!
        }
        nonSyncPending().lazySet(null) // release notification ownership

        // clean up nulled out listeners
        nonSyncListeners.let {
            if (it is Array<*>) {
                nonSyncListeners = it.withoutNulls<Any>(null)
            }
        }
    }

    private fun nonSyncNotifyAll(old: T, new: T, diff: D) {
        // take them at the beginning of notification,
        // so we won't touch listeners added during this notification cycle
        val listeners = nonSyncListeners!!

        var i = 0
        if (listeners !is Array<*>) { // single listener
            @Suppress("UNCHECKED_CAST")
            notify(listeners as LISTENER, old, new, diff)

            /*listeners = nonSyncListeners!! // Wrong behaviour: check for new listeners and notify them all
            if (listeners is Array<*>)
                i = 1 // [wrong] transformed to array during notification, start from the second item
            else*/
            return
        }

        // array of listeners

        while (true) { // [wrong] size may change, so we can't use Kotlin's for loop (iterator) here

            if (i == listeners.size)
                break

            val listener = listeners[i]
            if (listener != null) {
                @Suppress("UNCHECKED_CAST")
                notify(listener as LISTENER, old, new, diff)
            }
            i++

            /*[wrong] if (i == listeners.size) listeners = nonSyncListeners as Array<*> */
        }
    }

    protected abstract fun pack(new: @UnsafeVariance T, diff: D): UPDATE
    protected abstract fun unpackValue(packed: UPDATE): T
    protected abstract fun unpackDiff(packed: UPDATE): @UnsafeVariance D

    protected abstract fun notify(listener: LISTENER, old: @UnsafeVariance T, new: @UnsafeVariance T, diff: D)

    internal fun checkThread() {
        if (Thread.currentThread() !== thread)
            throw RuntimeException("${Thread.currentThread()} is not allowed to touch this Property: " +
                    "it is single-threaded and confined to $thread. " +
                    "For concurrent access, use concurrentPropertyOf(…)")
    }

    protected fun concAddChangeListenerInternal(onChange: LISTENER) {
        val old = concState().getUndUpdate {
            it.withListener(onChange)
        }
        if (old.listeners.all { it == null } && old.pending.isEmpty()) {
            changeObservedStateTo(true)
        } // else if pending.isNotEmpTy() => we're currently notifying and won't change observed state
    }

    protected fun nonSyncAddChangeListenerInternal(onChange: LISTENER) {
        checkThread()
        val pending = nonSyncPending().get()
        if (pending === null) { // currently not notifying
            nonSyncReallyAddChangeListener(onChange, pending)
        } else { // currently notifying — postpone subscription
            nonSyncPending().lazySet(pending.with(ConcListeners.AddListener(onChange)))
        }
    }

    private fun nonSyncReallyAddChangeListener(onChange: LISTENER, pending: Array<Any?>?) {
        when (val listeners = nonSyncListeners) {
            null -> {
                nonSyncListeners = onChange
                changeObservedStateTo(true)
            }
            // this typecheck will break if single-threaded DiffProperty will be added! [1/2]
            is Function2<*, *, *> -> nonSyncListeners = arrayOf(listeners, onChange)
            is Array<*> -> {
                if (pending != null) {
                    // notifying now, expand array without structural changes
                    nonSyncListeners = listeners.with(onChange)
                    // don't check observed state, just assume it's 'true' during notification
                } else {
                    listeners as Array<Any?>
                    // not notifying, we can do anything we want
                    when (val insIdx = listeners.compact(/* remove nulls */)) {
                        -1 -> {// no nulls, grow
                            nonSyncListeners = listeners.with(onChange)
                        }

                        0 -> {// drop array, especially if it is SingleNull instance which must not be mutated
                            nonSyncListeners = onChange
                            changeObservedStateTo(true)
                        }

                        else -> {// we have some room in the existing array
                            listeners[insIdx] = onChange
                        }
                    }
                }
            }
            else -> throw AssertionError()
        }
    }

    internal inline fun removeChangeListenerWhere(predicate: (LISTENER) -> Boolean) {
        if (thread == null) { // concurrent
            var hasOthers = false
            concState().update { prev ->
                hasOthers = false
                var victimIdx = -1
                val prevLis = prev.listeners
                for (i in prevLis.indices) {
                    val listener = prevLis[i]
                            ?: continue

                    if (predicate(listener)) {
                        if (victimIdx == -1) {
                            victimIdx = i
                            if (hasOthers) {
                                break
                                // victim found, others detected,
                                // nothing to do here
                            }
                        }
                    } else {
                        hasOthers = true
                        if (victimIdx != -1) {
                            break
                            // others detected, victim found,
                            // that's all
                        }
                    }
                }

                if (victimIdx >= 0) {
                    prev.withoutListenerAt(victimIdx)
                } else {
                    // at this point we don't mind this flag
                    // since we're gonna remove a listener which has not been actually added,
                    // leaving observed state unchanged
                    hasOthers = true

                    // we must also search in ConcListeners.pending since it may contain listeners, too
                    val prevPending = prev.pending
                    var pendingVictimIdx = -1
                    for (i in prevPending.indices) {
                        val listener = (prevPending[i] as? ConcListeners.AddListener<LISTENER>)?.listener
                                ?: continue

                        if (predicate(listener)) {
                            if (pendingVictimIdx == -1) {
                                pendingVictimIdx = i
                                break
                            }
                        }
                    }

                    if (pendingVictimIdx < 0)
                        return // not found in both arrays, give up without changes

                    prev.withoutPendingAt(pendingVictimIdx)
                }
            }

            if (!hasOthers) {
                changeObservedStateTo(false)
            }
        } else { // single-thread
            checkThread()
            when (val listeners = nonSyncListeners) {
                null -> return
                // this typecheck will break if single-threaded DiffProperty will be added! [2/2]
                is Function2<*, *, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    if (!predicate(listeners as LISTENER))
                        return

                    nonSyncListeners =
                            if (nonSyncPending().get() == null) null
                            else SingleNull.also { check(it[0] === null) }

                    changeObservedStateTo(false)
                }
                is Array<*> -> {
                    val idx = listeners.indexOfFirst {
                        @Suppress("UNCHECKED_CAST")
                        it != null && predicate(it as LISTENER)
                    }
                    if (idx < 0) return

                    @Suppress("UNCHECKED_CAST")
                    (listeners as Array<Any?>)[idx] = null

                    if (listeners.all { it == null }) changeObservedStateTo(false)
                }
                else -> throw AssertionError()
            }
        }
    }

//    private var observed = false // test-only
    internal fun changeObservedStateTo(obsState: Boolean) {
        if (thread === null) {
            concChangeObservedStateTo(obsState)
        } else {
//            if (observed == obsState) error("observed state is already $obsState")
//            observed = obsState

            // a bit of recursion does not look like a real problem
            observedStateChanged(obsState)
        }
    }

    private fun concChangeObservedStateTo(obsState: Boolean) {
        var prev: ConcListeners<LISTENER, UPDATE>
        var next: ConcListeners<LISTENER, UPDATE>
        do {
            prev = concState().get()
            while (prev.transitionLocked) {
                // if we can't transition for external reasons (e. g. in ConcMutableProperty), just wait until this ends
                Thread.yield()
                prev = concState().get()
            }

            if (prev.nextObservedState == obsState) {
                // do nothing if we're either transitioning to current state or already there
                return
            }

            next = prev.startTransition()
        } while (!concState().compareAndSet(prev, next))

        if (prev.transitioningObservedState) {
            // do nothing if this method is already on the stack somewhere
            return
        }

        // mutex: transitioning = true and won't be reset outside of this method
        var nextState = obsState
        while (true) {
            // first, perform transition to this state:
            observedStateChanged(nextState) // this is a potentially long-running callback

            // we're done, but state may have changed
            val prevState = concState().updateUndGet { prev ->
                prev.continueTransition(nextState)
            }

            if (prevState.transitioningObservedState) { // state have changed concurrently
                check(!nextState == prevState.nextObservedState)
                nextState = prevState.nextObservedState
            } else { // state committed, mutex released
                return
            }
        }
    }

    /* intentionally does not contain try..catch */
    internal inline fun withLockedTransition(block: () -> Unit) {
        while (!tryLockTransition()) Thread.yield()
        block()
        unlockTransition()
    }

    internal fun tryLockTransition(): Boolean {
        val prev = concState().get()
        if (prev.transitionLocked || prev.transitioningObservedState) {
            return false
        }

        return concState().compareAndSet(prev, prev.flippedTransitionLock())
    }

    internal fun unlockTransition() {
        val old = concState().getUndUpdate(ConcListeners<LISTENER, UPDATE>::flippedTransitionLock)
        check(old.transitionLocked)
    }

    /*...not overridden in [ConcMutableDiffProperty], because it is not mapped and cannot be bound.
     * This callback can be called only once at a time (i. e. under mutex) for a single property,
     * and with transitionLocked=false */
    internal open fun observedStateChanged(observed: Boolean) {}

    @Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
    internal inline fun concState() =
            this as AtomicReference<ConcListeners<LISTENER, UPDATE>>

    @Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
    internal inline fun nonSyncPending() =
            this as AtomicReference<Array<Any?>?> // = UPDATE | AddListener<LISTENER>

}
