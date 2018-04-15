package net.aquadc.properties.internal

import net.aquadc.properties.Property
import net.aquadc.properties.executor.PlatformExecutors
import net.aquadc.properties.executor.ScheduledDaemonHolder
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater

/**
 * Notifies subscribers about changes with a delay,
 * swallowing useless updates.
 */
class ConcDebouncedProperty<out T>(
        private val original: Property<T>,
        delay: Long,
        unit: TimeUnit
) : PropNotifier<T>(null) {

    @Suppress("UNUSED") @Volatile
    private var pending: Pair<T, ScheduledFuture<*>>? = null

    override val value: T
        get() = original.value

    init {
        check(original.mayChange)
        check(original.isConcurrent)

        // take `listeners` into local
        original.addChangeListener { old, new ->
            pendingUpdater<T>().update(this) {
                val reallyOld = if (it == null) old else {
                    val f = it.second
                    if (!f.isDone) f.cancel(false)
                    it.first
                }

                Pair(reallyOld,
                        ScheduledDaemonHolder.scheduledDaemon.schedule({
                            valueChanged(reallyOld, new, null)
                        }, delay, unit)
                )
            }
        }
    }

    override fun addChangeListener(onChange: (old: T, new: T) -> Unit) =
            super.addChangeListener(ConfinedChangeListener(
                    PlatformExecutors.executorForCurrentThread(),
                    onChange
            ))

    /**
     * Note: this will remove first occurrence of [onChange],
     * no matter on which executor it was subscribed.
     */
    override fun removeChangeListener(onChange: (old: T, new: T) -> Unit) {
        if (thread == null) {
            concStateUpdater().update(this) {
                it.withoutListenerAt(
                        it.listeners.indexOfFirst {
                            (it as ConfinedChangeListener<*>).actual === onChange
                        }
                )
            }
        } else {
            // almost copy of PropNotifier.removeChangeListener
            checkThread()
            val listeners = nonSyncListeners
            when (listeners) {
                null -> return
                is ConfinedChangeListener<*> -> {
                    if (listeners.actual !== onChange)
                        return

                    nonSyncListeners = if (nonSyncPendingUpdater().get(this) == null) null else SingleNull
                }
                is Array<*> -> {
                    val idx = listeners.indexOfFirst { (it as ConfinedChangeListener<*>).actual === onChange }
                    if (idx < 0) return
                    if (nonSyncPendingUpdater().get(this) != null) {
                        // notifying now. Null this listener out, that's all
                        @Suppress("UNCHECKED_CAST")
                        (listeners as Array<Any?>)[idx] = null
                    } else {
                        nonSyncListeners = listeners.copyOfWithout(idx, null)
                    }
                }
                else -> throw AssertionError()
            }
        }
    }

    private companion object {
        @JvmField
        val pendingUpdater: AtomicReferenceFieldUpdater<ConcDebouncedProperty<*>, Pair<*, *>> =
                AtomicReferenceFieldUpdater.newUpdater(ConcDebouncedProperty::class.java, Pair::class.java, "pending")

        @Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
        inline fun <T> pendingUpdater() =
                pendingUpdater as AtomicReferenceFieldUpdater<ConcDebouncedProperty<T>, Pair<T, ScheduledFuture<*>>?>
    }

}
