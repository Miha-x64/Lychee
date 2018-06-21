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
 * swallowing too frequent updates.
 */
@PublishedApi
internal class `Debounced-`<out T>(
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
            prop: `Debounced-`<T>
    ) : WeakReference<`Debounced-`<T>>(prop), ChangeListener<T> {

        @JvmField
        internal var hard: `Debounced-`<T>? = null

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

    @Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
    private companion object {
        @JvmField internal val pendingUpdater: AtomicReferenceFieldUpdater<`Debounced-`<*>, Pair<*, *>> =
                AtomicReferenceFieldUpdater.newUpdater(`Debounced-`::class.java, Pair::class.java, "pending")

        private inline fun <T> pendingUpdater() =
                pendingUpdater as AtomicReferenceFieldUpdater<`Debounced-`<T>, Pair<T, ScheduledFuture<*>>?>

        @JvmField internal val valueUpdater: AtomicReferenceFieldUpdater<`Debounced-`<*>, Any?> =
                AtomicReferenceFieldUpdater.newUpdater(`Debounced-`::class.java, Any::class.java, "value")

        private inline fun <T> valueUpdater() =
                valueUpdater as AtomicReferenceFieldUpdater<`Debounced-`<T>, Any?>
    }

}
