package net.aquadc.properties.internal

import net.aquadc.properties.ChangeListener
import net.aquadc.properties.Property
import net.aquadc.properties.addUnconfinedChangeListener
import net.aquadc.properties.executor.*
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater


internal class MappedProperty<in O, out T>(
        @JvmField internal val original: Property<@UnsafeVariance O>,
        @JvmField internal val map: (O) -> T,
        mapOn: Worker
) : PropNotifier<T>(threadIfNot(original.isConcurrent)) {

    init {
        check(original.mayChange)
    }

    @Volatile @JvmField @Suppress("UNUSED", "MemberVisibilityCanBePrivate") // used from inner class
    internal var valueRef: @UnsafeVariance T = this as T // 'this' means 'not observed'

    private val originalChanged: ChangeListener<O> = when {
        // simple concurrent binding: notify where changed
        original.isConcurrent -> MapWhenChanged(mapOn, map) { new ->
            val old = valueUpdater<T>().getAndSet(this, new)
            valueChanged(old, new, null)
        }

        // simple non-synchronized binding
        mapOn === InPlaceWorker -> { _: O, new: O ->
            val tOld = valueRef
            val tNew = map(new)
            valueUpdater<T>().lazySet(this, tNew)
            valueChanged(tOld, tNew, null)
        }

        // not concurrent, but must be computed on a worker;
        // must also bring result to this thread
        else -> MapWhenChanged(mapOn, map,
                ConsumeOn(PlatformExecutors.executorForCurrentThread()) { new ->
                    val old = valueRef
                    valueUpdater<T>().lazySet(this, new)
                    valueChanged(old, new, null)
                }
        )
    }

    override fun observedStateChanged(observed: Boolean) {
        if (observed) {
            val mapped = map(original.value)
            valueUpdater<T>().eagerOrLazySet(this, thread, mapped)
            original.addUnconfinedChangeListener(originalChanged)
        } else {
            original.removeChangeListener(originalChanged)
            valueUpdater<T>().eagerOrLazySet(this, thread, this as T)
        }
    }

    override val value: T
        get() {
            if (thread !== null) checkThread()
            val value = valueUpdater<T>().get(this)

            // if not observed, calculate on demand
            return if (value === this) map(original.value) else value
        }

    private companion object {
        @JvmField
        val ValueUpdater: AtomicReferenceFieldUpdater<MappedProperty<*, *>, Any?> =
                AtomicReferenceFieldUpdater.newUpdater(MappedProperty::class.java, Any::class.java, "valueRef")

        @Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
        inline fun <T> valueUpdater() =
                ValueUpdater as AtomicReferenceFieldUpdater<MappedProperty<*, T>, T>
    }

}
