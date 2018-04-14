package net.aquadc.properties.internal

import net.aquadc.properties.Property

/**
 * Represents a property which transforms `original`'s value.
 */
class UnsMappedProperty<in O, out T>(
        original: Property<O>,
        private val transform: (O) -> T
) : PropNotifier<T>(Thread.currentThread()) {

    init {
        check(original !is ImmutableReferenceProperty)

        original.addChangeListener { old, new ->
            onChange(old, new)
        }
    }

    @Suppress("MemberVisibilityCanBePrivate") // produce no synthetic accessors
    internal fun onChange(old: O, new: O) {
        val tOld = transform(old)
        val tNew = transform(new)
        valueRef = tNew
        valueChanged(tOld, tNew, null)
    }

    private var valueRef: T = transform(original.value)

    override val value: T
        get() {
            checkThread()
            return valueRef
        }

}
