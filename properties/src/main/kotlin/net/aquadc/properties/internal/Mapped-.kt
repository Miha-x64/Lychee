package net.aquadc.properties.internal

import androidx.annotation.RestrictTo
import net.aquadc.properties.ChangeListener
import net.aquadc.properties.Property
import net.aquadc.properties.addUnconfinedChangeListener
import net.aquadc.properties.executor.*

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
open class `Mapped-`<in O, out T>(
        @JvmField @JvmSynthetic val original: Property<@UnsafeVariance O>,
        @JvmField @JvmSynthetic internal val map: (O) -> T,
        mapOn: Worker<*>
) : `Notifier-1AtomicRef`<T, @UnsafeVariance T>(original.isConcurrent, unset()) {

    init {
        check(original.mayChange || this is `TimeMapped-`)
        // TimeMappedProperty must update with time, even when original is immutable
    }

    protected val originalChanged: ChangeListener<O> = when {
        // simple concurrent binding: notify where changed
        original.isConcurrent -> MapWhenChanged(mapOn, map) { new ->
            val old = refUpdater().getAndSet(this, new)
            valueChanged(old, new, null)
        }

        // simple non-synchronized binding
        mapOn === InPlaceWorker -> { _: O, new: O ->
            val tOld = ref
            val tNew = map(new)
            refUpdater().lazySet(this, tNew)
            valueChanged(tOld, tNew, null)
        }

        // not concurrent, but must be computed on a worker;
        // must also bring result to this thread
        else -> MapWhenChanged(mapOn, map,
                ConsumeOn(PlatformExecutors.requireCurrent()) { new ->
                    val old = ref
                    refUpdater().lazySet(this, new)
                    valueChanged(old, new, null)
                }
        )
    }

    override fun observedStateChanged(observed: Boolean) {
        if (observed) {
            val mapped = map(original.value)
            refUpdater().eagerOrLazySet(this, thread, mapped)
            original.addUnconfinedChangeListener(originalChanged)
        } else {
            original.removeChangeListener(originalChanged)
            refUpdater().eagerOrLazySet(this, thread, unset())
            (originalChanged as? MapWhenChanged<*, *, *>)?.cancel()
        }
    }

    override val value: T
        get() {
            if (thread !== null) checkThread()
            val value = ref

            // if not observed, calculate on demand
            return if (value === Unset) map(original.value) else value
        }

}
