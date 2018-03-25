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

    @Volatile @Suppress("UNCHECKED_CAST") // it's safe, I PROVE IT
    private var listeners: Triple<
            @ParameterName("notifying") Boolean,
            @ParameterName("listeners") Listeners<T>,
            @ParameterName("pendingValues") Array<Any?>?
            > = InitialListeners as Triple<Boolean, Listeners<T>, Array<Any?>>

    final override fun addChangeListener(onChange: ChangeListener<T>) {
        listenersUpdater<T>().update(this) { (notifying, listeners, pending) ->
            Triple(notifying, listeners.with(onChange), pending)
        }
    }
    final override fun removeChangeListener(onChange: ChangeListener<T>) {
        listenersUpdater<T>().update(this) { (notifying, listeners, pending) ->
            val idx = listeners.indexOf(onChange)
            if (idx < 0) return

            val newListeners = if (notifying) {
                listeners.clone().also { it[idx] = null }
            } else {
                listeners.copyOfWithout(idx)
            }

            Triple(notifying, newListeners, pending)
        }
    }

    /**
     * Sophisticated thing, see [net.aquadc.properties.internal.UnsListeners.valueChanged].
     */
    protected fun valueChanged(old: Any?, new: Any?) {
        old as T; new as T

        val (_, listeners) = listenersUpdater<T>().get(this)

        // if we have no one to notify, just give up
        if (listeners.isEmpty()) return

        var meNotifying = false

        listenersUpdater<T>().update(this) { (notifying, listeners, pending) ->
            if (notifying) {
                meNotifying = false
                Triple(notifying, listeners, pending.with(new))
            } else {
                meNotifying = true
                Triple(true, listeners, pending)
            }
        }

        if (!meNotifying) return // other `valueChanged` is on the stack.

        // notifying is true now
        notify(old, new)

        var older = new
        while (true) {
            val state = listenersUpdater<T>().get(this)
            val (updating, listeners, pending) = state
            check(updating)

            if (pending.isEmpty()) {
                // all pending notified, reset 'notifying' flag and remove nulled out listeners
                val update = Triple(false, listeners.withoutNulls(), /* empty array */pending)
                if (listenersUpdater<T>().compareAndSet(this, state, update)) {
                    return
                }
            } else {
                val update = Triple(true, listeners, pending.copyOfWithout(0))
                if (listenersUpdater<T>().compareAndSet(this, state, update)) {
                    // now we've removed pending[0] from queue, let's notify
                    val newer = pending[0]
                    notify(older, newer)
                    older = newer
                }
            }
        }
    }

    private fun notify(old: Any?, new: Any?) {
        old as T; new as T

        var i = 0
        // read volatile size on every step, it's important!
        while (i < listenersUpdater<T>().get(this).second.size) {
            listenersUpdater<T>().get(this).second[i]?.invoke(old, new)
            i++
        }
    }

    private companion object {
        @JvmField
        val NoListeners = emptyArray<ChangeListener<*>>()
        @JvmField
        val InitialListeners = Triple(false, NoListeners, emptyArray<Any?>())

        @JvmField
        val listenersUpdater: AtomicReferenceFieldUpdater<ConcPropListeners<*>, Triple<*, *, *>> =
                AtomicReferenceFieldUpdater.newUpdater(ConcPropListeners::class.java, Triple::class.java, "listeners")
        @Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
        inline fun <T> listenersUpdater() =
                listenersUpdater as AtomicReferenceFieldUpdater<BaseConcProperty<T>, Triple<Boolean, Listeners<T>, Array<Any?>>>
    }

}

private typealias Listeners<T> = Array<ChangeListener<T>?>
