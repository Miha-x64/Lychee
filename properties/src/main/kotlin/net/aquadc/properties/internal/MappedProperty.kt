package net.aquadc.properties.internal

import net.aquadc.properties.Property
import net.aquadc.properties.executor.*
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater


class MappedProperty<in O, out T>(
        original: Property<O>,
        map: (O) -> T,
        mapOn: Worker
) : PropNotifier<T>(threadIfNot(original.isConcurrent)) {

    init {
        check(original.mayChange)
    }

    @Volatile @Suppress("UNUSED")
    private var valueRef = map(original.value)

    init {
        when {
            // simple concurrent binding: notify where changed
            original.isConcurrent -> original.addChangeListener(MapWhenChanged(mapOn, map) { new ->
                val old = valueUpdater<T>().getAndSet(this, new)
                valueChanged(old, new, null)
            })

            // simple non-synchronized binding
            mapOn === InPlaceWorker -> original.addChangeListener { _: O, new: O ->
                val tOld = valueRef
                val tNew = map(new)
                valueUpdater<T>().lazySet(this, tNew)
                valueChanged(tOld, tNew, null)
            }

            // not concurrent, but must be computed on a worker;
            // must also bring result to this thread
            else -> original.addChangeListener(
                MapWhenChanged(mapOn, map,
                        ConsumeOn(PlatformExecutors.executorForCurrentThread()) { new ->
                            val old = valueRef
                            valueUpdater<T>().lazySet(this, new)
                            valueChanged(old, new, null)
                        }
                )
            )
        }
    }

    override val value: T
        get() = valueUpdater<T>().get(this)

    private companion object {
        @JvmField
        val ValueUpdater: AtomicReferenceFieldUpdater<MappedProperty<*, *>, Any?> =
                AtomicReferenceFieldUpdater.newUpdater(MappedProperty::class.java, Any::class.java, "valueRef")

        @Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
        inline fun <T> valueUpdater() =
                ValueUpdater as AtomicReferenceFieldUpdater<MappedProperty<*, T>, T>
    }

}
