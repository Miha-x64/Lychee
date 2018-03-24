package net.aquadc.properties.internal

import net.aquadc.properties.Property
import net.aquadc.properties.executor.PlatformExecutors
import net.aquadc.properties.executor.ScheduledDaemonHolder
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * Notifies subscribers about changes with a delay,
 * swallowing useless updates.
 */
class UnsDebouncedProperty<out T>(
        private val original: Property<T>,
        private val delay: Long,
        private val unit: TimeUnit
) : UnsListeners<T>() {

    private val executor = PlatformExecutors.executorForCurrentThread()
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

            pending = Pair(reallyOld, ScheduledDaemonHolder.scheduledDaemon.schedule({
                executor.execute {
                    valueChanged(reallyOld, new)
                }
            }, delay, unit))
        }
    }

    override fun getValue(): T {
        checkThread()
        return original.getValue()
    }

}
