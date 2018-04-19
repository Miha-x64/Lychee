package net.aquadc.properties.internal

import net.aquadc.properties.Property

@PublishedApi
internal class ImmutableReferenceProperty<out T>(
        override val value: T
) : Property<T> {

    override val mayChange: Boolean get() = false
    override val isConcurrent: Boolean get() = true

    override fun addChangeListener(onChange: (old: T, new: T) -> Unit) = Unit

    override fun removeChangeListener(onChange: (old: T, new: T) -> Unit) = Unit

}
