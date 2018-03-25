package net.aquadc.properties.internal

import net.aquadc.properties.Property


class UnsMultiMappedProperty<in A, out T>(
        properties: Iterable<Property<A>>,
        private val transform: (List<A>) -> T
): UnsListeners<T>() {

    // hm... I could use Array instead of List here for performance reasons
    private var _value: Pair<List<A>, T>
    init {
        properties.forEachIndexed { i, prop ->
            if (prop.mayChange) {
                prop.addChangeListener { _, new -> set(i, new) }
            }
        }

        val values = properties.map(Property<A>::getValue)
        _value = Pair(values, transform(values))
    }

    override fun getValue(): T {
        checkThread()
        return _value.second
    }

    @Suppress("MemberVisibilityCanBePrivate") // produce no synthetic accessors
    internal fun set(index: Int, value: A) {
        val old = _value
        val values = old.first
        val changed = values.mapIndexed { i, v -> if (i == index) value else v }
        val new = Pair(changed, transform(changed))
        _value = new

        val ov = old.second
        val nv = new.second
        valueChanged(ov, nv)
    }

}
