package net.aquadc.properties.internal

import net.aquadc.properties.Property

class UnsynchronizedMappedReferenceProperty<O, out T>(
        private val original: Property<O>,
        private val transform: (O) -> T
) : Property<T> {

    private val thread = Thread.currentThread()

    init {
        if (original is ImmutableReferenceProperty)
            throw IllegalArgumentException("immutable property $original should not be mapped")
    }

    private val listeners = ArrayList<(T, T) -> Unit>()

    init {
        original.addChangeListener { old, new ->
            val tOld = transform(old)
            val tNew = transform(new)
            if (tOld !== tNew) {
                listeners.forEach { it(transform(old), transform(new)) }
            }
        }
    }

    override val value: T get() {
        checkThread(thread)
        return transform(original.value)
    }

    override val mayChange: Boolean get() {
        checkThread(thread)
        return true
    }

    override val isConcurrent: Boolean get() {
        checkThread(thread)
        return false
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
