package net.aquadc.properties.internal

import net.aquadc.properties.ChangeListener
import net.aquadc.properties.Property
import net.aquadc.properties.addUnconfinedChangeListener

@PublishedApi
internal class `MultiMapped-`<in A, out T>(
        properties: Collection<Property<A>>,
        private val transform: (List<A>) -> T
) : `Notifier-1AtomicRef`<T, @UnsafeVariance T>(
        properties.any { it.isConcurrent && it.mayChange }, unset()
        // if at least one property is concurrent, we must be ready that
        // it will notify us from a random thread
), ChangeListener<A> {

    private val properties: Array<Property<A>>

    init {
        @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
        this.properties = (properties as java.util.Collection<*>).toArray(arrayOfNulls(properties.size))
    }

    override val value: T
        get() {
            if (thread != null) checkThread()
            val value = ref
            return if (value === Unset) transformed() else value
        }

    override fun invoke(_old: A, _new: A) {
        var old: T
        var new: T

        do {
            old = ref
            new = transformed()
        } while (!cas(old, new))

        valueChanged(old, new, null)
    }

    private fun cas(old: T, new: T): Boolean = if (thread === null) {
        refUpdater().compareAndSet(this, old, new)
    } else {
        refUpdater().lazySet(this, new)
        true
    }

    override fun observedStateChanged(observed: Boolean) {
        if (observed) {
            val value = transform.invoke(AList(properties.size) { this.properties[it].value })
            refUpdater().eagerOrLazySet(this, thread, value)
            properties.forEach { if (it.mayChange) it.addUnconfinedChangeListener(this) }
        } else {
            properties.forEach { if (it.mayChange) it.removeChangeListener(this) }
            refUpdater().eagerOrLazySet(this, thread, unset())
        }
    }

    private fun transformed(): T =
            transform(AList(properties.size) { this.properties[it].value })

}
