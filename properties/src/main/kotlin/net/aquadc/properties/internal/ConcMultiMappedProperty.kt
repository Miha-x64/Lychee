package net.aquadc.properties.internal

import net.aquadc.properties.Property
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater


class ConcMultiMappedProperty<in A, out T>(
        properties: Iterable<Property<A>>,
        private val transform: (List<A>) -> T
) : BaseConcProperty<T>() {

    @Volatile @Suppress("UNUSED")
    private var valueRef: Pair<List<A>, T>
    init {
        properties.forEachIndexed { i, prop ->
            if (prop.mayChange) {
                prop.addChangeListener { _, new -> set(i, new) }
            }
        }

        val values = properties.map { it.value }
        valueRef = values to transform(values)
    }

    override val value: T
        get() = valueUpdater<A, T>().get(this).second

    private fun set(index: Int, value: A) {
        var old: Pair<List<A>, T>
        var new: Pair<List<A>, T>


        do {
            old = valueUpdater<A, T>().get(this)

            val values = old.first
            val changed = values.mapIndexed { i, v -> if (i == index) value else v }
            val transformed = transform(changed)
            new = Pair(changed, transformed)
        } while (!valueUpdater<A, T>().compareAndSet(this, old, new))

        val ov = old.second
        val nv = new.second
        listeners.forEach { it(ov, nv) }
    }

    private val listeners = CopyOnWriteArrayList<(T, T) -> Unit>()
    override fun addChangeListener(onChange: (old: T, new: T) -> Unit) {
        listeners.add(onChange)
    }
    override fun removeChangeListener(onChange: (old: T, new: T) -> Unit) {
        listeners.remove(onChange)
    }

    @Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
    private companion object {
        val ValueUpdater: AtomicReferenceFieldUpdater<ConcMultiMappedProperty<*, *>, Pair<*, *>> =
                AtomicReferenceFieldUpdater.newUpdater(ConcMultiMappedProperty::class.java, Pair::class.java, "valueRef")

        inline fun <A, T> valueUpdater() =
                ValueUpdater as AtomicReferenceFieldUpdater<ConcMultiMappedProperty<A, T>, Pair<List<A>, T>>
    }

}
