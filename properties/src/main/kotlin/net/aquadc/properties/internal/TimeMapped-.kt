package net.aquadc.properties.internal

import net.aquadc.properties.Property
import net.aquadc.properties.executor.InPlaceWorker
import net.aquadc.properties.executor.ScheduledDaemonHolder
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture

@PublishedApi
internal class `TimeMapped-`<O, T>(
        original: Property<O>,
        map: (O) -> T,
        private val schedulePeriodically: (ScheduledExecutorService, T, Runnable) -> ScheduledFuture<*>
) : `Mapped-`<O, T>(original, map, InPlaceWorker), Runnable {

    override fun run() {
        if (isBeingObserved()) {
            val value = original.value
            originalChanged(unset(), value) // impl detail: 'old' will be ignored
        }
    }

    @Volatile
    private var future: ScheduledFuture<*>? = null

    override fun observedStateChanged(observed: Boolean) {
        super.observedStateChanged(observed)
        val ref = ref
        future = if (observed && ref !== unset<T>())
            schedulePeriodically(ScheduledDaemonHolder.scheduledDaemon, ref, this)
        else
            future!!.cancel(false).let { null }
    }

}
