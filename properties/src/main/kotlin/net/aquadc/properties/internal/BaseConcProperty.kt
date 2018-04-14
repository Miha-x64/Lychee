package net.aquadc.properties.internal

import net.aquadc.properties.ChangeListener
import net.aquadc.properties.Property
import net.aquadc.properties.diff.internal.ConcMutableDiffProperty
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater

// I don't like implementation inheritance, but it is more lightweight than composition.

/**
 * Base class containing concurrent props' listeners.
 * Despite class is public, this is private API.
 * Used by [ConcPropNotifier] and [ConcMutableDiffProperty].
 */
abstract class ConcPropListeners<out T, in D, LISTENER : Any, UPDATE> : Property<T> {

    final override val mayChange: Boolean
        get() = true

    final override val isConcurrent: Boolean
        get() = true

    @Volatile @Suppress("UNUSED")
    private var listeners: ConcListeners<LISTENER, UPDATE> = ConcListeners.NoListeners

    /**
     * Sophisticated thing, see [net.aquadc.properties.internal.UnsListeners.valueChanged].
     */
    protected fun valueChanged(old: @UnsafeVariance T, new: @UnsafeVariance T, diff: D) {
        val oldListeners = enqueueUpdate(old, new, diff)
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
            val state = listenersUpdater().get(this)

            val current = state.pendingValues[0] // take a pending value from prev state
            val currentValue = unpackValue(current)
            notifyAll(prev, currentValue, unpackDiff(current))
            prev = currentValue

            // `pending[0]` is delivered, now remove it from our 'queue'

            /*
             * taking fresh state is safe because the only one thread/function is allowed to notify.
             * Just remove our 'queue' head, but let anyone add pending values or listeners
             */
            val next = listenersUpdater()
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
    private fun enqueueUpdate(old: @UnsafeVariance T, new: @UnsafeVariance T, diff: D): ConcListeners<LISTENER, UPDATE>? {
        var prev: ConcListeners<LISTENER, UPDATE>
        var next: ConcListeners<LISTENER, UPDATE>
        do {
            prev = listenersUpdater().get(this)

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

            if (listenersUpdater().compareAndSet(this, prev, next)) {
                return prev
            }

        } while (true)
    }
    protected abstract fun pack(new: @UnsafeVariance T, diff: D): UPDATE
    protected abstract fun unpackValue(packed: UPDATE): T
    protected abstract fun unpackDiff(packed: UPDATE): @UnsafeVariance D

    private fun notifyAll(old: T, new: T, diff: D) {
        var i = 0
        var listeners = listenersUpdater().get(this).listeners
        while (i < listeners.size) {
            val listener = listeners[i]
            if (listener != null) {
                notify(listener, old, new, diff)
            }

            // read volatile listeners on every step, they may be added or nulled out!
            listeners = listenersUpdater().get(this).listeners
            i++
        }
    }

    protected abstract fun notify(listener: LISTENER, old: @UnsafeVariance T, new: @UnsafeVariance T, diff: D)

    protected companion object {
        @JvmField
        val listenersUpdater: AtomicReferenceFieldUpdater<ConcPropListeners<*, *, *, *>, ConcListeners<*, *>> =
                AtomicReferenceFieldUpdater.newUpdater(ConcPropListeners::class.java, ConcListeners::class.java, "listeners")
        @Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST", "UNUSED")
        inline fun <T, D, LISTENER : Any, PACKED> ConcPropListeners<T, D, LISTENER, PACKED>.listenersUpdater() =
                listenersUpdater as AtomicReferenceFieldUpdater<ConcPropListeners<T, D, LISTENER, PACKED>, ConcListeners<LISTENER, PACKED>>
    }

}

/**
 * Base class containing concurrent notification logic.
 * Despite class is public, this is private API.
 */
abstract class ConcPropNotifier<out T> :
        ConcPropListeners<T, Nothing?, ChangeListener<@UnsafeVariance T>, @UnsafeVariance T>() {

    override fun addChangeListener(onChange: ChangeListener<T>) =
            listenersUpdater().update(this) { it.withListener(onChange) }

    override fun removeChangeListener(onChange: ChangeListener<T>) =
            listenersUpdater().update(this) { it.withoutListener(onChange) }

    final override fun pack(new: @UnsafeVariance T, diff: Nothing?): T =
            new

    final override fun unpackValue(packed: @UnsafeVariance T): T =
            packed

    final override fun unpackDiff(packed: @UnsafeVariance T): Nothing? =
            null

    final override fun notify(listener: ChangeListener<T>, old: @UnsafeVariance T, new: @UnsafeVariance T, diff: Nothing?) =
            listener(old, new)

}
