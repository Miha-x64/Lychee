package net.aquadc.properties.internal

import net.aquadc.properties.ChangeListener
import net.aquadc.properties.Property
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater

@PublishedApi
internal class MultiMappedProperty<in A, out T>(
        properties: Collection<Property<A>>,
        private val transform: (List<A>) -> T
) : PropNotifier<T>(
        threadIfNot(properties.any { it.isConcurrent && it.mayChange })
        // if at least one property is concurrent, we must be ready that
        // it will notify us from a random thread
), ChangeListener<A> {

    private val properties: Array<Property<A>>

    @Volatile @Suppress("UNUSED")
    private var valueRef: T = this as T

    init {
        @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
        this.properties = (properties as java.util.Collection<*>).toArray(arrayOfNulls(properties.size))
    }

    override val value: T
        get() {
            if (thread != null) checkThread()
            val value = valueUpdater<A, T>().get(this)
            return if (value === this) transformed() else value
        }

    override fun invoke(_old: A, _new: A) {
        var old: T
        var new: T

        do {
            old = valueUpdater<A, T>().get(this)
            new = transformed()
        } while (!cas(old, new))

        valueChanged(old, new, null)
    }

    private fun cas(old: T, new: T): Boolean = if (thread === null) {
        valueUpdater<A, T>().compareAndSet(this, old, new)
    } else {
        valueUpdater<A, T>().lazySet(this, new)
        true
    }

    override fun observedStateChangedWLocked(observed: Boolean) {
        if (observed) {
            val value = transform.invoke(List(properties.size) { this.properties[it].value })
            valueUpdater<A, T>().eagerOrLazySet(this, thread, value)
            properties.forEach { if (it.mayChange) it.addChangeListener(this) }
        } else {
            properties.forEach { if (it.mayChange) it.removeChangeListener(this) }
            valueUpdater<A, T>().eagerOrLazySet(this, thread, this as T)
        }
    }

    private fun transformed(): T =
            transform(List(properties.size) { this.properties[it].value })

    private companion object {
        @JvmField
        val ValueUpdater: AtomicReferenceFieldUpdater<MultiMappedProperty<*, *>, *> =
                AtomicReferenceFieldUpdater.newUpdater(MultiMappedProperty::class.java, Any::class.java, "valueRef")

        @Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
        inline fun <A, T> valueUpdater() =
                ValueUpdater as AtomicReferenceFieldUpdater<MultiMappedProperty<A, T>, T>
    }

}
