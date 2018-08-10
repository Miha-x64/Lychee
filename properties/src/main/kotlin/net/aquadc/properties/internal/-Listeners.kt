package net.aquadc.properties.internal

import android.support.annotation.RestrictTo
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
     */
    protected fun valueChanged(old: @UnsafeVariance T, new: @UnsafeVariance T, diff: D) {
        if (thread == null) concValueChanged(old, new, diff)
        else nonSyncValueChanged(old, new, diff)
    }

    private fun concValueChanged(old: @UnsafeVariance T, new: @UnsafeVariance T, diff: D) {
        val oldListeners = concEnqueueUpdate(old, new, diff)
                ?: return // nothing to notify

        if (oldListeners.pendingValues.isNotEmpty())
            return // other [valueChanged] is on the stack or in parallel,
        // [new] was added to pending values and will be delivered soon

        /*
         * [pendingValues] is not empty now, it's a kind of lock
         * new values were added to pending list, let's deliver all the pending values,
         * including those which will appear during the notification
         */

        var prev = old
        while (true) {
            val state = concState().get() as ConcListeners<LISTENER, UPDATE>

            val current = state.pendingValues[0] // take a pending value from prev state
            val currentValue = unpackValue(current)
            concNotifyAll(prev, currentValue, unpackDiff(current))
            prev = currentValue

            // `pending[0]` is delivered, now remove it from our 'queue'

            /*
             * taking fresh state is safe because the only one thread/function is allowed to notify.
             * Just remove our 'queue' head, but let anyone add pending values or listeners
             */
            val next = concState()
                    .updateUndGet(ConcListeners<LISTENER, UPDATE>::next)

            if (next.pendingValues.isEmpty()) {
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

            if (prev.pendingValues.isNotEmpty()) {
                // other thread or stack performs notification at the moment

                /*
                 * if another thread made value CAS earlier,
                 * but have not made CAS on listeners yet, just wait
                 */

                if (unpackValue(prev.pendingValues.last()) !== old) {
                    Thread.yield() // a bit of awful programming, yay!
                    // wait until other thread set its update, we can't help here
                    continue
                }
            }

            /*
             * at this point, either
             * prev.pendingValues.isNotEmpty() && prev.pendingValues.last().first == old
             * or
             * prev.pendingValues.isEmpty()
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
        while (i < listeners.size) {
            val listener = listeners[i]
            if (listener != null) {
                notify(listener, old, new, diff)
            }

            // read volatile listeners on every step, they may be added or nulled out!
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
            nonSyncPending().lazySet(newValues as Array<UPDATE>)

            // and let deeper version of this method pick it up.
            return
        }

        @Suppress("UNCHECKED_CAST") // this means 'don't notify directly, add to the queue!'
        nonSyncPending().lazySet(EmptyArray as Array<UPDATE>)
        // pendingValues is empty array now

        // now we own notification process

        nonSyncNotifyAll(old, new, diff)

        var pendingValues = nonSyncPending().get()!!
        var i = 0
        var older = new
        while (true) { // now take enqueued values set by notified properties
            if (i == pendingValues.size)
                break // real end of queue, nothing to do here

            val newer = pendingValues[i]
            val newerValue = unpackValue(newer)
            nonSyncNotifyAll(older, newerValue, unpackDiff(newer))
            older = newerValue
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
        var listeners = nonSyncListeners!!

        var i = 0
        if (listeners !is Array<*>) { // single listener
            @Suppress("UNCHECKED_CAST")
            notify(listeners as LISTENER, old, new, diff)

            listeners = nonSyncListeners!!
            if (listeners is Array<*>) {
                i = 1 // transformed to array during notification, start from second item
            } else {
                return
            }
        }

        // array of listeners

        while (true) { // size may change, so we can't use Kotlin's for loop (iterator) here
            listeners as Array<*> // smart-cast doesn't work when assignment below exists

            if (i == listeners.size)
                break

            val listener = listeners[i]
            if (listener != null) {
                @Suppress("UNCHECKED_CAST")
                notify(listener as LISTENER, old, new, diff)
            }
            i++

            if (i == listeners.size)
                listeners = nonSyncListeners as Array<*>
        }
    }

    protected abstract fun pack(new: @UnsafeVariance T, diff: D): UPDATE
    protected abstract fun unpackValue(packed: UPDATE): T
    protected abstract fun unpackDiff(packed: UPDATE): @UnsafeVariance D

    protected abstract fun notify(listener: LISTENER, old: @UnsafeVariance T, new: @UnsafeVariance T, diff: D)

    internal fun checkThread() {
        if (Thread.currentThread() !== thread)
            throw RuntimeException("${Thread.currentThread()} is not allowed to touch this property since it was created in $thread.")
    }

    protected fun addChangeListenerInternal(onChange: LISTENER) {
        if (thread == null) {
            concAddChangeListenerInternal(onChange)
        } else {
            nonSyncAddChangeListenerInternal(onChange)
        }
    }

    protected fun concAddChangeListenerInternal(onChange: LISTENER) {
        val old = concState().getUndUpdate {
            it.withListener(onChange)
        }
        if (old.listeners.all { it == null }) {
            changeObservedStateTo(true)
        }
    }

    protected fun nonSyncAddChangeListenerInternal(onChange: LISTENER) {
        checkThread()
        val listeners = nonSyncListeners
        when (listeners) {
            null -> {
                nonSyncListeners = onChange
                changeObservedStateTo(true)
            }
            is Function2<*, *, *> -> nonSyncListeners = arrayOf(listeners, onChange)
            is Array<*> -> {
                if (nonSyncPending().get() != null) {
                    // notifying now, expand array without structural changes
                    nonSyncListeners = listeners.with(onChange)
                    if (listeners.all { it == null }) {
                        changeObservedStateTo(true)
                    }
                } else {
                    // not notifying, we can do anything we want
                    val insIdx = listeners.compact() // remove nulls
                    when (insIdx) {
                        -1 -> {// no nulls, grow
                            nonSyncListeners = listeners.with(onChange)
                        }

                        0 -> {// drop array, especially if it is SingleNull instance which must not be mutated
                            nonSyncListeners = onChange
                            changeObservedStateTo(true)
                        }

                        else -> {// we have some room in the existing array
                            @Suppress("UNCHECKED_CAST")
                            (listeners as Array<Any?>)[insIdx] = onChange
                        }
                    }
                }
            }
            else -> throw AssertionError()
        }
    }

    internal inline fun removeChangeListenerWhere(predicate: (LISTENER) -> Boolean) {
        if (thread == null) {
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

                if (victimIdx < 0) {
                    return
                }

                prev.withoutListenerAt(victimIdx)
            }

            if (!hasOthers) {
                changeObservedStateTo(false)
            }
        } else {
            checkThread()
            val listeners = nonSyncListeners
            when (listeners) {
                null -> return
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

    private fun changeObservedStateTo(obsState: Boolean) {
        if (thread === null) {
            concChangeObservedStateTo(obsState)
        } else {
            // a bit of recursion does not look like a real problem
            observedStateChanged(obsState)
        }
    }

    private fun concChangeObservedStateTo(obsState: Boolean) {
        val firsState = concState().getUndUpdate { prev ->
            if (prev.nextObservedState == obsState) {
                // do nothing if we're either transitioning to current state or already there
                return
            }

            prev.startTransition()
        }

        if (firsState.transitioningObservedState) {
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

    /*...not overridden in [ConcMutableDiffProperty], because it is not mapped and cannot be bound. */
    internal open fun observedStateChanged(observed: Boolean) {}

    @Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
    internal inline fun concState() =
            this as AtomicReference<ConcListeners<LISTENER, UPDATE>>

    @Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
    internal inline fun nonSyncPending() =
            this as AtomicReference<Array<UPDATE>?>

}
