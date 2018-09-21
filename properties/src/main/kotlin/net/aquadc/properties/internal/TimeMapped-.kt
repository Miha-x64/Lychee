package net.aquadc.properties.internal

import net.aquadc.properties.Property
import net.aquadc.properties.executor.InPlaceWorker
import net.aquadc.properties.executor.ScheduledDaemonHolder
import java.util.concurrent.ScheduledExecutorService

@PublishedApi
internal class `TimeMapped-`<O, T>(
        original: Property<O>,
        map: (O) -> T,
        private val scheduleNext: (ScheduledExecutorService, T, Runnable) -> Unit
) : `Mapped-`<O, T>(original, map, InPlaceWorker) {

    private val periodical = object : Runnable {
        override fun run() {
            if (isBeingObserved()) {
                val value = original.value
                originalChanged(value, value) // impl detail: 'old' will be ignored
                val ref = ref
                if (ref !== unset<T>()) {
                    scheduleNext(ScheduledDaemonHolder.scheduledDaemon, ref, this)
                }
            }
        }
    }

    override fun observedStateChanged(observed: Boolean) {
        super.observedStateChanged(observed)
        val ref = ref
        if (observed && ref !== unset<T>()) {
            scheduleNext(ScheduledDaemonHolder.scheduledDaemon, ref, periodical)
        }
    }

}
