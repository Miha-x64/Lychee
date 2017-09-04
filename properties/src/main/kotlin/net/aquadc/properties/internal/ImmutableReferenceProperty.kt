package net.aquadc.properties.internal

import net.aquadc.properties.Property
import net.aquadc.properties.immutablePropertyOf

class ImmutableReferenceProperty<out T>(
        override val value: T
) : Property<T> {

    override fun addChangeListener(onChange: (old: T, new: T) -> Unit) = Unit

    override fun removeChangeListener(onChange: (old: T, new: T) -> Unit) = Unit

    override fun <R> map(transform: (T) -> R): Property<R> = immutablePropertyOf(transform(value))

}
