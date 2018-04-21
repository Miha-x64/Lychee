package net.aquadc.properties.internal

import net.aquadc.properties.ChangeListener
import net.aquadc.properties.Property
import net.aquadc.properties.diff.internal.ConcMutableDiffProperty
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater

// I don't like implementation inheritance, but it is more lightweight than composition.

/**
 * Base class containing concurrent props' listeners.
 * Despite class is public, this is private API.
 * Used by [PropNotifier] and [ConcMutableDiffProperty].
 * @property thread our thread, or null, if this property is concurrent
 */
abstract class PropListeners<out T, in D, LISTENER : Any, UPDATE>(
        @JvmField protected val thread: Thread?
) : Property<T> {

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
     * This has type [ConcListeners]<LISTENER, UPDATE> for concurrent properties
     * and `UPDATE[]?` for non-synchronized ones;
     * single-thread properties store listeners separately.
     */
    @Volatile @Suppress("UNUSED")
    private var state: Any? = if (thread == null) ConcListeners.NoListeners else null

    /**
     * Holds listeners of a non-synchronized property. Unused, if this is a concurrent one.
     * When unused, this introduces no sadness:
     * on 64-bit HotSpot with compressed OOPS and 8-byte object alignment,
     * instances of this class have size of 24 bytes.
     * Without [nonSyncListeners] this space would be occupied by padding.
     */
    @JvmField protected var nonSyncListeners: Any? = null

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
            val state = concStateUpdater().get(this) as ConcListeners<LISTENER, UPDATE>

            val current = state.pendingValues[0] // take a pending value from prev state
            val currentValue = unpackValue(current)
            concNotifyAll(prev, currentValue, unpackDiff(current))
            prev = currentValue

            // `pending[0]` is delivered, now remove it from our 'queue'

            /*
             * taking fresh state is safe because the only one thread/function is allowed to notify.
             * Just remove our 'queue' head, but let anyone add pending values or listeners
             */
            val next = concStateUpdater()
                    .updateUndGet(this, ConcListeners<LISTENER, UPDATE>::next)

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
            prev = concStateUpdater().get(this)

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

            if (concStateUpdater().compareAndSet(this, prev, next)) {
                return prev
            }

        } while (true)
    }

    private fun concNotifyAll(old: T, new: T, diff: D) {
        var i = 0
        var listeners = concStateUpdater().get(this).listeners
        while (i < listeners.size) {
            val listener = listeners[i]
            if (listener != null) {
                notify(listener, old, new, diff)
            }

            // read volatile listeners on every step, they may be added or nulled out!
            listeners = concStateUpdater().get(this).listeners
            i++
        }
    }


    private fun nonSyncValueChanged(old: @UnsafeVariance T, new: @UnsafeVariance T, diff: D) {
        // if we have no one to notify, just give up
        if (nonSyncListeners == null) return

        nonSyncPendingUpdater().get(this)?.let { pendingValues ->
            // This method is already on the stack, somewhere deeper.
            // Just push the new value...
            val newValues = pendingValues.with(pack(new, diff))

            @Suppress("UNCHECKED_CAST")
            nonSyncPendingUpdater().lazySet(this, newValues as Array<UPDATE>)

            // and let deeper version of this method pick it up.
            return
        }

        @Suppress("UNCHECKED_CAST") // this means 'don't notify directly, add to the queue!'
        nonSyncPendingUpdater().lazySet(this, ConcListeners.EmptyArray as Array<UPDATE>)
        // pendingValues is empty array now

        // now we own notification process

        nonSyncNotifyAll(old, new, diff)

        var pendingValues = nonSyncPendingUpdater().get(this)!!
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
                pendingValues = nonSyncPendingUpdater().get(this)!!
        }
        nonSyncPendingUpdater().lazySet(this, null) // release notification ownership

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
                listeners = nonSyncListeners!! as Array<*>
        }
    }

    protected abstract fun pack(new: @UnsafeVariance T, diff: D): UPDATE
    protected abstract fun unpackValue(packed: UPDATE): T
    protected abstract fun unpackDiff(packed: UPDATE): @UnsafeVariance D

    protected abstract fun notify(listener: LISTENER, old: @UnsafeVariance T, new: @UnsafeVariance T, diff: D)

    protected fun checkThread() {
        if (Thread.currentThread() !== thread)
            throw RuntimeException("${Thread.currentThread()} is not allowed to touch this property since it was created in $thread.")
    }

    internal companion object {
        @JvmField val updater: AtomicReferenceFieldUpdater<PropListeners<*, *, *, *>, Any> =
                AtomicReferenceFieldUpdater.newUpdater(PropListeners::class.java, Any::class.java, "state")

        @JvmField val SingleNull = arrayOfNulls<Any>(1)

        @Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST", "UNUSED")
        inline fun <T, D, LISTENER : Any, PACKED> PropListeners<T, D, LISTENER, PACKED>.concStateUpdater() =
                updater as AtomicReferenceFieldUpdater<PropListeners<T, D, LISTENER, PACKED>, ConcListeners<LISTENER, PACKED>>

        @Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST", "UNUSED")
        inline fun <T, D, LISTENER : Any, PACKED> PropListeners<T, D, LISTENER, PACKED>.nonSyncPendingUpdater() =
                updater as AtomicReferenceFieldUpdater<PropListeners<T, D, LISTENER, PACKED>, Array<PACKED>?>
    }

}

/**
 * Base class containing concurrent notification logic.
 * Despite class is public, this is private API.
 */
abstract class PropNotifier<out T>(thread: Thread?) :
        PropListeners<T, Nothing?, ChangeListener<@UnsafeVariance T>, @UnsafeVariance T>(thread) {

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

    override fun addChangeListener(onChange: ChangeListener<T>) {
        if (thread == null) {
            val old = concStateUpdater().getUndUpdate(this) { it.withListener(onChange) }
            if (old.listeners.all { it == null }) observedStateChanged(true)
        } else {
            checkThread()
            val listeners = nonSyncListeners
            when (listeners) {
                null -> {
                    nonSyncListeners = onChange
                    observedStateChanged(true)
                }
                is Function2<*, *, *> -> nonSyncListeners = arrayOf(listeners, onChange)
                is Array<*> -> {
                    if (nonSyncPendingUpdater().get(this) != null) {
                        // notifying now, expand array without structural changes
                        nonSyncListeners = listeners.with(onChange)
                        if (listeners.all { it == null }) observedStateChanged(true)
                    } else {
                        // not notifying, we can do anything we want
                        val insIdx = listeners.compact() // remove nulls
                        when (insIdx) {
                            -1 -> {// no nulls, grow
                                nonSyncListeners = listeners.with(onChange)
                            }

                            0 -> {// drop array, especially if it is SingleNull instance which must not be mutated
                                nonSyncListeners = onChange
                                observedStateChanged(true)
                            }

                            else -> // we have some room in the existing array
                                @Suppress("UNCHECKED_CAST")
                                (listeners as Array<Any?>)[insIdx] = onChange
                        }
                    }
                }
                else -> throw AssertionError()
            }
        }
    }

    override fun removeChangeListener(onChange: ChangeListener<T>) {
        removeChangeListenerWhere { it === onChange }
    }

    internal inline fun removeChangeListenerWhere(predicate: (ChangeListener<@UnsafeVariance T>) -> Boolean) {
        if (thread == null) {
            val new = concStateUpdater().updateUndGet(this) {
                val victimIdx = it.listeners.indexOfFirst { it != null && predicate(it) }
                if (victimIdx < 0) return
                it.withoutListenerAt(victimIdx)
            }
            // it's guaranteed that we've successfully removed something
            if (new.listeners.all { it == null }) observedStateChanged(false)
        } else {
            checkThread()
            val listeners = nonSyncListeners
            when (listeners) {
                null -> return
                is Function2<*, *, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    if (!predicate(listeners as ChangeListener<T>))
                        return

                    nonSyncListeners =
                            if (nonSyncPendingUpdater().get(this) == null) null
                            else SingleNull.also { check(it[0] === null) }

                    observedStateChanged(false)
                }
                is Array<*> -> {
                    val idx = listeners.indexOfFirst {
                        @Suppress("UNCHECKED_CAST")
                        it != null && predicate(it as ChangeListener<T>)
                    }
                    if (idx < 0) return

                    @Suppress("UNCHECKED_CAST")
                    (listeners as Array<Any?>)[idx] = null

                    if (listeners.all { it == null }) observedStateChanged(false)
                }
                else -> throw AssertionError()
            }
        }
    }

    protected open fun observedStateChanged(observed: Boolean) {}

    final override fun pack(new: @UnsafeVariance T, diff: Nothing?): T =
            new

    final override fun unpackValue(packed: @UnsafeVariance T): T =
            packed

    final override fun unpackDiff(packed: @UnsafeVariance T): Nothing? =
            null

    final override fun notify(listener: ChangeListener<T>, old: @UnsafeVariance T, new: @UnsafeVariance T, diff: Nothing?) =
            listener(old, new)

}

internal fun threadIfNot(concurrent: Boolean): Thread? =
        if (concurrent) null else Thread.currentThread()
