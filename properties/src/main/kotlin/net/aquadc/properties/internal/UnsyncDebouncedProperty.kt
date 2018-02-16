package net.aquadc.properties.internal

import net.aquadc.properties.Property
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * Notifies subscribers about changes with a delay,
 * swallowing useless updates.
 */
class UnsyncDebouncedProperty<out T>(
        private val original: Property<T>,
        private val delay: Long,
        private val unit: TimeUnit
) : Property<T> {

    private val thread = Thread.currentThread()
    private val executor = PlatformExecutors.executorForThread(thread)
    private var pending: Pair<T, ScheduledFuture<*>>? = null

    init {
        check(original.mayChange)
        check(!original.isConcurrent)

        original.addChangeListener { old, new ->
            val it = pending
            val reallyOld = if (it == null) old else {
                val f = it.second
                if (!f.isDone) f.cancel(false)
                it.first
            }

            pending = Pair(reallyOld, ConcurrentDebouncedProperty.scheduled.schedule({
                executor.execute {
                    listeners.notifyAll(reallyOld, new)
                }
            }, delay, unit))
        }
    }

    override val value: T
        get() {
            checkThread(thread)
            return original.value
        }

    override val mayChange: Boolean
        get() {
            checkThread(thread)
            return true
        }

    override val isConcurrent: Boolean
        get() {
            checkThread(thread)
            return false
        }

    private var listeners: Any? = null
    override fun addChangeListener(onChange: (old: T, new: T) -> Unit) {
        listeners = listeners.plus(onChange)
    }
    override fun removeChangeListener(onChange: (old: T, new: T) -> Unit) {
        listeners = listeners.minus(onChange)
    }

}
