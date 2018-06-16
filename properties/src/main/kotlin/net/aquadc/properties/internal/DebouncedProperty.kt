package net.aquadc.properties.internal

import net.aquadc.properties.ChangeListener
import net.aquadc.properties.Property
import net.aquadc.properties.addUnconfinedChangeListener
import net.aquadc.properties.executor.ConfinedChangeListener
import net.aquadc.properties.executor.PlatformExecutors
import net.aquadc.properties.executor.ScheduledDaemonHolder
import java.lang.ref.WeakReference
import java.util.concurrent.Executor
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater

/**
 * Notifies subscribers about changes with a delay,
 * swallowing useless updates.
 */
@PublishedApi
internal class DebouncedProperty<out T>(
        original: Property<T>,
        private val delay: Long,
        private val unit: TimeUnit
) : PropNotifier<T>(threadIfNot(original.isConcurrent)) {

    @Suppress("UNUSED") @Volatile
    private var pending: Pair<T, ScheduledFuture<*>>? = null

    init {
        check(original.mayChange)
    }

    @Volatile
    override var value: @UnsafeVariance T = original.value
        get() {
            if (thread !== null) checkThread()
            return field
        }
        internal set // accessed from inner class

    private val observer = Observer(original, this).also {
        original.addUnconfinedChangeListener(it)
    }

    internal fun originalChanged(old: @UnsafeVariance T, new: @UnsafeVariance T) {
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

    override fun addChangeListener(onChange: ChangeListener<T>) {
        addChangeListenerInternal(ConfinedChangeListener(PlatformExecutors.executorForCurrentThread(), onChange))
    }

    override fun addChangeListenerOn(executor: Executor, onChange: ChangeListener<T>) {
        addChangeListenerInternal(ConfinedChangeListener(executor, onChange))
    }

    override fun observedStateChanged(observed: Boolean) {
        if (observed) {
            observer.hard = this
        } else {
            observer.hard = null
        }
    }

    private class Observer<T>(
            private val original: Property<T>,
            prop: DebouncedProperty<T>
    ) : WeakReference<DebouncedProperty<T>>(prop), ChangeListener<T> {

        @JvmField
        internal var hard: DebouncedProperty<T>? = null

        override fun invoke(old: T, new: T) {
            val actual = get()
            if (actual == null) {
                check(hard == null) // when actual property GCed, it's impossible to have a hard ref
                original.removeChangeListener(this)
                return // it's the end, we've GCed
            }

            actual.originalChanged(old, new)
        }

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
