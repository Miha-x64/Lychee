package net.aquadc.properties.internal

import net.aquadc.properties.ChangeListener
import net.aquadc.properties.Property
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater


class MultiMappedProperty<in A, out T>(
        properties: Collection<Property<A>>,
        private val transform: (List<A>) -> T
) : PropNotifier<T>(
        if (properties.all(Property<A>::isConcurrent)) null else Thread.currentThread()
), ChangeListener<A> {

    private val properties: Array<Property<A>>

    @Volatile @Suppress("UNUSED")
    private var valueRef: T

    init {
        @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
        this.properties = (properties as java.util.Collection<*>).toArray(arrayOfNulls(properties.size))
        this.properties.forEach { if (it.mayChange) it.addChangeListener(this) }

        valueRef = transform.invoke(List(properties.size) { this.properties[it].value })
    }

    override val value: T
        get() = valueUpdater<A, T>().get(this)

    override fun invoke(_old: A, _new: A) {
        var old: T
        var new: T

        do {
            old = valueUpdater<A, T>().get(this)

            val values = List(properties.size) { properties[it].value }
            new = transform.invoke(values)
        } while (!cas(old, new))

        valueChanged(old, new, null)
    }

    private fun cas(old: T, new: T): Boolean = if (thread === null) {
        valueUpdater<A, T>().compareAndSet(this, old, new)
    } else {
        valueUpdater<A, T>().lazySet(this, new)
        true
    }

    private companion object {
        @JvmField
        val ValueUpdater: AtomicReferenceFieldUpdater<MultiMappedProperty<*, *>, *> =
                AtomicReferenceFieldUpdater.newUpdater(MultiMappedProperty::class.java, Any::class.java, "valueRef")

        @Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
        inline fun <A, T> valueUpdater() =
                ValueUpdater as AtomicReferenceFieldUpdater<MultiMappedProperty<A, T>, T>
    }

}
