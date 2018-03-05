package net.aquadc.properties.internal

import net.aquadc.properties.Property

class ImmutableReferenceProperty<out T>(
        private val value: T
) : Property<T> {

    override fun getValue(): T =
            value

    override val mayChange: Boolean get() = false
    override val isConcurrent: Boolean get() = true

    override fun addChangeListener(onChange: (old: T, new: T) -> Unit) = Unit

    override fun removeChangeListener(onChange: (old: T, new: T) -> Unit) = Unit

}
