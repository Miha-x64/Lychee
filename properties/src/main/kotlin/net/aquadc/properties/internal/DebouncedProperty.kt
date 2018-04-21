package net.aquadc.properties.internal

import net.aquadc.properties.ChangeListener
import net.aquadc.properties.Property
import net.aquadc.properties.executor.ConfinedChangeListener
import net.aquadc.properties.executor.PlatformExecutors
import net.aquadc.properties.executor.ScheduledDaemonHolder
import java.util.concurrent.Executor
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater

/**
 * Notifies subscribers about changes with a delay,
 * swallowing useless updates.
 */
internal class DebouncedProperty<out T>(
        private val original: Property<T>,
        private val delay: Long,
        private val unit: TimeUnit
) : PropNotifier<T>(threadIfNot(original.isConcurrent)), ChangeListener<@UnsafeVariance T> {

    @Suppress("UNUSED") @Volatile
    private var pending: Pair<T, ScheduledFuture<*>>? = null

    private lateinit var executor: Executor

    init {
        check(original.mayChange)
        if (thread !== null) executor = PlatformExecutors.executorForCurrentThread()
    }

    @Volatile
    override var value: @UnsafeVariance T = this as T // this means 'not observed'
        get() {
            if (thread !== null) checkThread()
            val v = field
            return if (v === this) original.value else v
        }
        private set

    override fun invoke(old: @UnsafeVariance T, new: @UnsafeVariance T) {
        var prev: Pair<T, ScheduledFuture<*>>?
        var next: Pair<T, ScheduledFuture<*>>
        do {
            prev = pendingUpdater<T>().get(this)

            // swallow intermediate values
            val reallyOld = if (prev == null) old else {
                val (value, future) = prev
                when {
                    future.isDone -> old // already notified, take newer old value
                    future.cancel(false) -> value // canceled successfully, take older old value
                    else -> old // can't cancel, take newer old value
                }
            }

            next = Pair(
                    reallyOld,
                    ScheduledDaemonHolder.scheduledDaemon.schedule({
                        if (thread === null) value = new
                        else valueUpdater<T>().lazySet(this, new)

                        valueChanged(reallyOld, new, null)
                    }, delay, unit)
            )
        } while (!casPending(prev, next))
    }

    private fun casPending(prev: Pair<T, ScheduledFuture<*>>?, next: Pair<T, ScheduledFuture<*>>): Boolean =
            if (thread === null) {
                pendingUpdater<T>().compareAndSet(this, prev, next)
            } else {
                pendingUpdater<T>().lazySet(this, next)
                true
            }

    override fun addChangeListener(onChange: (old: T, new: T) -> Unit) =
            super.addChangeListener(ConfinedChangeListener(
                    PlatformExecutors.executorForCurrentThread(),
                    onChange
            ))

    override fun observedStateChangedWLocked(observed: Boolean) {
        if (observed) {
            value = original.value
            original.addChangeListener(this)
        } else {
            original.removeChangeListener(this)
            value = this as T
        }
    }

    /**
     * Note: this will remove first occurrence of [onChange],
     * no matter on which executor it was subscribed.
     */
    override fun removeChangeListener(onChange: (old: T, new: T) -> Unit) {
        removeChangeListenerWhere { (it as ConfinedChangeListener<*>).actual === onChange }
    }

    private companion object {
        @JvmField
        val pendingUpdater: AtomicReferenceFieldUpdater<DebouncedProperty<*>, Pair<*, *>> =
                AtomicReferenceFieldUpdater.newUpdater(DebouncedProperty::class.java, Pair::class.java, "pending")

        @Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
        inline fun <T> pendingUpdater() =
                pendingUpdater as AtomicReferenceFieldUpdater<DebouncedProperty<T>, Pair<T, ScheduledFuture<*>>?>

        @JvmField
        val valueUpdater: AtomicReferenceFieldUpdater<DebouncedProperty<*>, Any?> =
                AtomicReferenceFieldUpdater.newUpdater(DebouncedProperty::class.java, Any::class.java, "value")

        @Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
        inline fun <T> valueUpdater() =
                valueUpdater as AtomicReferenceFieldUpdater<DebouncedProperty<T>, Any?>
    }

}
