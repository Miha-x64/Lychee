package net.aquadc.properties.internal

import net.aquadc.properties.ChangeListener
import net.aquadc.properties.Property
import net.aquadc.properties.executor.PlatformExecutors
import net.aquadc.properties.executor.ScheduledDaemonHolder
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executor
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater

/**
 * Notifies subscribers about changes with a delay,
 * swallowing useless updates.
 */
class ConcDebouncedProperty<out T>(
        private val original: Property<T>,
        private val delay: Long,
        private val unit: TimeUnit
) : BaseConcProperty<T>() { // todo: upgrade listeners!

    @Suppress("UNUSED") @Volatile
    private var pending: Pair<T, ScheduledFuture<*>>? = null

    init {
        check(original.mayChange)
        check(original.isConcurrent)

        original.addChangeListener { old, new ->
            pendingUpdater<T>().update(this) {
                val reallyOld = if (it == null) old else {
                    val f = it.second
                    if (!f.isDone) f.cancel(false)
                    it.first
                }

                Pair(reallyOld,
                        ScheduledDaemonHolder.scheduledDaemon.schedule({
                            listeners.forEach {
                                it.first.execute { it.second(reallyOld, new) }
                            }
                        }, delay, unit)
                )
            }
        }
    }

    override fun getValue(): T =
            original.getValue()

    private val listeners = CopyOnWriteArrayList<Pair<Executor, ChangeListener<T>>>()
    override fun addChangeListener(onChange: (old: T, new: T) -> Unit) {
        listeners.add(PlatformExecutors.executorForCurrentThread() to onChange)
    }
    override fun removeChangeListener(onChange: (old: T, new: T) -> Unit) {
        listeners.firstOrNull { it.second == onChange }?.let { listeners.remove(it) }
    }

    companion object {
        private val pendingUpdater =
                AtomicReferenceFieldUpdater.newUpdater(ConcDebouncedProperty::class.java, Pair::class.java, "pending")

        @Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
        private inline fun <T> pendingUpdater() =
                pendingUpdater as AtomicReferenceFieldUpdater<ConcDebouncedProperty<T>, Pair<T, ScheduledFuture<*>>?>
    }

}
