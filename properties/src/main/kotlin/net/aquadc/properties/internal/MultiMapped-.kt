package net.aquadc.properties.internal

import net.aquadc.properties.ChangeListener
import net.aquadc.properties.Property
import net.aquadc.properties.addUnconfinedChangeListener

@PublishedApi
internal class `MultiMapped-`<in A, out T>(
        properties: Collection<Property<A>>,
        private val transform: (List<A>) -> T
) : `Notifier-1AtomicRef`<T, Any?>(
        properties.all(Property<A>::isConcurrent), unset()
        // if at least one property is single-threaded, we can't give any concurrent guarantees,
        // e. g. because adding first subscriber from wrong thread will lead to subscription on original and to crash
) {

    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    private val properties: Array<Property<A>> =
            (properties as java.util.Collection<*>).toArray(arrayOfNulls(properties.size))

    private val listeners = Array<ChangeListener<A>>(this.properties.size) { idx ->
        { _, new ->
            patch(idx, new)
        }
    }

    override val value: T
        get() {
            if (thread != null) checkThread()
            val value = ref
            return if (value === Unset) transform(AList(properties.size) { this.properties[it].value })
                   else (value as Array<Any?>).last() as T
        }

    @JvmSynthetic internal fun patch(index: Int, new: A) {
        var prevValue: T
        var oldVals: Array<Any?>
        var newVals: Array<Any?>
        do {
            oldVals = ref as Array<Any?>
            prevValue = oldVals.last() as T
            newVals = if (isConcurrent) oldVals.clone() else oldVals
            newVals[index] = new
            newVals[newVals.size - 1] = transform(SmallerList(newVals) as List<A>)
        } while (!cas(oldVals, newVals))

        valueChanged(prevValue, newVals.last() as T, null)
    }

    private fun cas(old: Any?, new: Any?): Boolean = if (thread === null) {
        refUpdater().compareAndSet(this, old, new)
    } else {
        refUpdater().lazySet(this, new)
        true
    }

    override fun observedStateChanged(observed: Boolean) {
        if (observed) {
            val values = arrayOfNulls<Any>(properties.size + 1)
            for (i in properties.indices) {
                values[i] = properties[i].value
            }
            values[properties.size] = transform(SmallerList(values) as List<A>)
            refUpdater().eagerOrLazySet(this, thread, values)
            // it's important to set the value *before* subscription

            val thisIsConc = isConcurrent
            for (i in properties.indices) {
                val prop = properties[i]
                val listener = listeners[i]
                if (thisIsConc) prop.addUnconfinedChangeListener(listener) // we expect updates from any thread
                else prop.addChangeListener(listener) // either listener or property here well be confined to a single thread
            }
        } else {
            for (i in properties.indices) {
                properties[i].removeChangeListener(listeners[i])
            }
            refUpdater().eagerOrLazySet(this, thread, unset())
        }
    }

    private class SmallerList<E>(
            private val array: Array<E>
    ) : AbstractList<E>() {

        override val size: Int
            get() = array.size - 1

        override fun get(index: Int): E {
            check(index < size)
            return array[index] // throw AIOOBE for negative indices, I don't mind
        }

    }

}
