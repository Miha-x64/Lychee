package net.aquadc.properties.internal

import net.aquadc.properties.ChangeListener
import net.aquadc.properties.Property
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater

// I don't like implementation inheritance, but it is more lightweight than composition.

/**
 * Base class for concurrent properties.
 * Despite class is public, this is private API.
 * Used by [ConcPropListeners] and [ConcDebouncedProperty].
 */
abstract class BaseConcProperty<out T> : Property<T> {

    final override val mayChange: Boolean
        get() = true

    final override val isConcurrent: Boolean
        get() = true

}

/**
 * Base class containing concurrent props' listeners.
 * Despite class is public, this is private API.
 * Used by [ConcPropNotifier] and `diff/ConcMutableDiffProperty`.
 */
abstract class ConcPropListeners<out T, in D, LISTENER : Any, UPDATE> : BaseConcProperty<T>() {

    @Volatile @Suppress("UNUSED")
    private var listeners: ConcListeners<LISTENER, UPDATE> = ConcListeners.NoListeners

    /**
     * Sophisticated thing, see [net.aquadc.properties.internal.UnsListeners.valueChanged].
     */
    protected fun valueChanged(old: @UnsafeVariance T, new: @UnsafeVariance T, diff: D) {
        val oldListeners = updateListeners(old, new, diff)
                ?: return // nothing to notify

        if (oldListeners.notifying)
            return // other `valueChanged` is on the stack, [new] was added to pending values

        /*
         * [notifying] is true now, it's a kind of lock
         * new values is added to pending list, let's deliver all the pending values
         */

        var prev = old
        while (true) {
            val state = listenersUpdater().get(this)

            if (state.notifying) {
                // let's deliver pending[0]
                val current = state.pendingValues[0] // take a pending value from prev state
                val currentValue = unpackValue(current)
                notifyAll(prev, currentValue, unpackDiff(current))
                prev = currentValue
            } else {
                // all pending notified, 'notifying' flag is reset, nulled out listeners are removed
                check(state.pendingValues.isEmpty())
                check(!state.listeners.contains(null))
                return // success! go home.
            }

            // it's time to remove `pending[0]` or clear `notifying` flag

            do {
                val actualState = listenersUpdater().get(this)
                // It's safe because the only one thread/function is allowed to notify.
                // Just remove our 'queue' head
            } while (!listenersUpdater().compareAndSet(this, actualState, actualState.next()))
        }
    }

    /**
     * Update listeners state.
     * If [ConcListeners.notifying] is `false`, will become `true`, old [ConcListeners] must be returned.
     * If [ConcListeners.notifying] it `true`, new value will be changed added, old value must be returned.
     * If there are no listeners, `null` must be returned.
     */
    private fun updateListeners(old: @UnsafeVariance T, new: @UnsafeVariance T, diff: D): ConcListeners<LISTENER, UPDATE>? {
        var prev: ConcListeners<LISTENER, UPDATE>
        var next: ConcListeners<LISTENER, UPDATE>
        do {
            prev = listenersUpdater().get(this)

            if (prev.listeners.isEmpty())
                return null // nothing to notify

            if (prev.notifying) {
                /*
                 * if another thread made value CAS earlier,
                 * but have not made CAS on listeners yet, just wait
                 */

                check(prev.pendingValues.isNotEmpty()) // while notifying, pending cannot be empty
                if (unpackValue(prev.pendingValues.last()) !== old) {
                    Thread.yield() // a bit of awful programming, yay!
                    // wait until other thread set its update, we can't help here
                    continue
                }
            }

            /*
             * at this point, either
             * prev.notifying && prev.pendingValues.last().first == old
             * or
             * !prev.notifying
             */

            next = prev.withNextValue(pack(new, diff))

            if (listenersUpdater().compareAndSet(this, prev, next))
                return prev

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

    final override fun addChangeListener(onChange: ChangeListener<T>) =
            listenersUpdater().update(this) { it.withListener(onChange) }

    final override fun removeChangeListener(onChange: ChangeListener<T>) =
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
