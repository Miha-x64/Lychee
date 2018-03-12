package net.aquadc.properties.internal

import net.aquadc.properties.Property

/**
 * Represents a property which transforms `original`'s value.
 */
class UnsMappedProperty<in O, out T>(
        original: Property<O>,
        private val transform: (O) -> T
) : UnsListeners<T>() {

    init {
        check(original !is ImmutableReferenceProperty)

        original.addChangeListener { old, new ->
            val tOld = transform(old)
            val tNew = transform(new)
            valueRef = tNew
            valueChanged(tOld, tNew)
        }
    }

    private var valueRef: T = transform(original.getValue())

    override fun getValue(): T {
        checkThread()
        return valueRef
    }

}
