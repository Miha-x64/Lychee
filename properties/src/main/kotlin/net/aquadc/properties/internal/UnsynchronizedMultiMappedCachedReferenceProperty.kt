package net.aquadc.properties.internal

import net.aquadc.properties.Property

class UnsynchronizedMultiMappedCachedReferenceProperty<A, out T>(
        properties: Iterable<Property<A>>,
        private val transform: (List<A>) -> T
): Property<T> {

    private val thread = Thread.currentThread()

    override val mayChange: Boolean get() {
        checkThread(thread)
        return true
    }
    override val isConcurrent: Boolean get() {
        checkThread(thread)
        return false
    }

    // hm... I could use Array instead of List here for performance reasons
    private var _value: Pair<List<A>, T>
    init {
        properties.forEachIndexed { i, prop ->
            if (prop.mayChange) {
                prop.addChangeListener { _, new -> set(i, new) }
            }
        }

        val values = properties.map { it.value }
        _value = Pair(values, transform(values))
    }

    override val value: T get() {
        checkThread(thread)
        return _value.second
    }

    private val listeners = ArrayList<(T, T) -> Unit>()

    private fun set(index: Int, value: A) {
        val old = _value
        val values = old.first
        val changed = values.mapIndexed { i, v -> if (i == index) value else v }
        val new = Pair(changed, transform(changed))
        _value = new

        if (new.second !== old.second) {
            val ov = old.second
            val nv = new.second
            listeners.forEach { it(ov, nv) }
        }
    }

    override fun addChangeListener(onChange: (old: T, new: T) -> Unit) {
        checkThread(thread)
        listeners.add(onChange)
    }

    override fun removeChangeListener(onChange: (old: T, new: T) -> Unit) {
        checkThread(thread)
        listeners.remove(onChange)
    }

}
