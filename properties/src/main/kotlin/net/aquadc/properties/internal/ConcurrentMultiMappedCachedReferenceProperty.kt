package net.aquadc.properties.internal

import net.aquadc.properties.Property
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

class ConcurrentMultiMappedCachedReferenceProperty<A, out T>(
        properties: Iterable<Property<A>>,
        private val transform: (List<A>) -> T
): Property<T> {

    override val mayChange: Boolean get() = true

    // hm... I could use Array instead of List here for performance reasons
    private val valueReference: AtomicReference<Pair<List<A>, T>>
    init {
        properties.forEachIndexed { i, prop ->
            if (prop.mayChange) {
                prop.addChangeListener { _, new -> set(i, new) }
            }
        }

        val values = properties.map { it.value }
        valueReference = AtomicReference(Pair(values, transform(values)))
    }

    override val value: T get() = valueReference.get().second

    private val listeners = CopyOnWriteArrayList<(T, T) -> Unit>()

    private fun set(index: Int, value: A) {
        var old: Pair<List<A>, T>
        var new: Pair<List<A>, T>


        do {
            old = valueReference.get()

            val values = old.first
            val changed = values.mapIndexed { i, v -> if (i == index) value else v }
            val transformed = transform(changed)
            new = Pair(changed, transformed)
        } while (!valueReference.compareAndSet(old, new))

        if (new !== old) {
            val ov = old.second
            val nv = new.second
            listeners.forEach { it(ov, nv) }
        }
    }

    override fun addChangeListener(onChange: (old: T, new: T) -> Unit) {
        listeners.add(onChange)
    }

    override fun removeChangeListener(onChange: (old: T, new: T) -> Unit) {
        listeners.remove(onChange)
    }

}
