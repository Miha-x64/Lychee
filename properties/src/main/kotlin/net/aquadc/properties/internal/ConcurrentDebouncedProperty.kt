package net.aquadc.properties.internal

import net.aquadc.properties.ChangeListener
import net.aquadc.properties.Property
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater

/**
 * Notifies subscribers about changes with a delay,
 * swallowing useless updates.
 */
class ConcurrentDebouncedProperty<out T>(
        private val original: Property<T>,
        private val delay: Long,
        private val unit: TimeUnit
) : Property<T> {

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

                Pair(reallyOld, scheduled.schedule({
                    listeners.forEach {
                        PlatformExecutors.executorForThread(it.first).execute { it.second(reallyOld, new) }
                    }
                }, delay, unit))
            }
        }
    }

    override val value: T
        get() = original.value

    override val mayChange: Boolean
        get() = true

    override val isConcurrent: Boolean
        get() = true

    private val listeners = CopyOnWriteArrayList<Pair<Thread, ChangeListener<T>>>()
    override fun addChangeListener(onChange: (old: T, new: T) -> Unit) {
        val thread = Thread.currentThread()
        PlatformExecutors.executorForThread(thread) // ensure such executor exists
        listeners.add(thread to onChange)
    }
    override fun removeChangeListener(onChange: (old: T, new: T) -> Unit) {
        listeners.firstOrNull { it.second == onChange }?.let { listeners.remove(it) }
    }

    companion object {
        private val pendingUpdater =
                AtomicReferenceFieldUpdater.newUpdater(ConcurrentDebouncedProperty::class.java, Pair::class.java, "pending")

        @Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
        private inline fun <T> pendingUpdater() =
                pendingUpdater as AtomicReferenceFieldUpdater<ConcurrentDebouncedProperty<T>, Pair<T, ScheduledFuture<*>>?>

        internal val scheduled = ScheduledThreadPoolExecutor(1, ThreadFactory { Thread(it).also { it.isDaemon = true } })
    }

}
