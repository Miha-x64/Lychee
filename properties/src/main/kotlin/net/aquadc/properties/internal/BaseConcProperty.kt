package net.aquadc.properties.internal

import net.aquadc.properties.ChangeListener
import net.aquadc.properties.Property
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater

/**
 * Base class for concurrent properties.
 * I don't like implementation inheritance, but it is more lightweight than composition.
 */
abstract class BaseConcProperty<out T> : Property<T> {

    final override val mayChange: Boolean
        get() = true

    final override val isConcurrent: Boolean
        get() = true

}

abstract class ConcPropListeners<out T> : BaseConcProperty<T>() {

    @Volatile
    private var listeners: ConcListeners = ConcListeners.NoListeners

    final override fun addChangeListener(onChange: ChangeListener<T>) {
        listenersUpdater<T>().update(this) { it.withListener(onChange) }
    }
    final override fun removeChangeListener(onChange: ChangeListener<T>) {
        listenersUpdater<T>().update(this) { it.withoutListener(onChange) }
    }

    /**
     * Sophisticated thing, see [net.aquadc.properties.internal.UnsListeners.valueChanged].
     */
    protected fun valueChanged(old: Any?, new: Any?) {
        val oldListeners = listenersUpdater<T>().iGetAndUpdate(this) {
            if (it.listeners.isEmpty()) {
                return // if we have no one to notify, just give up
            }

            it.withNextValue(new)
        }

        if (oldListeners.notifying) {
            return // other `valueChanged` is on the stack, [new] was added to pending values
        }

        // notifying is true now, it's a kind of lock
        notify(old, new)

        // done, but now we may have some pending values

        var prev = new
        while (true) {
            val state = listenersUpdater<T>().get(this)
            val update = state.next()
            if (!listenersUpdater<T>().compareAndSet(this, state, update)) {
                continue
            }

            if (update.notifying) {
                // now we've removed pending[0] from queue, let's notify
                val current = state.pendingValues[0] // take a pending value from prev state
                notify(prev, current)
                prev = current
            } else {
                // all pending notified, 'notifying' flag is reset, nulled out listeners are removed
                check(update.pendingValues.isEmpty())
                check(!update.listeners.contains(null))
                return // success! go home.
            }
        }
    }

    private fun notify(old: Any?, new: Any?) {
        var i = 0
        var listeners = listenersUpdater<T>().get(this).listeners
        while (i < listeners.size) {
            listeners[i]?.invoke(old, new)

            // read volatile listeners on every step, they may be added or nulled out!
            listeners = listenersUpdater<T>().get(this).listeners
            i++
        }
    }

    private companion object {
        @JvmField
        val listenersUpdater: AtomicReferenceFieldUpdater<ConcPropListeners<*>, ConcListeners> =
                AtomicReferenceFieldUpdater.newUpdater(ConcPropListeners::class.java, ConcListeners::class.java, "listeners")
        @Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
        inline fun <T> listenersUpdater() =
                listenersUpdater as AtomicReferenceFieldUpdater<BaseConcProperty<T>, ConcListeners>
    }

}
