package net.aquadc.properties.internal

import net.aquadc.properties.Property
import net.aquadc.properties.immutablePropertyOf

class ImmutableReferenceProperty<out T>(
        override val value: T
) : Property<T> {

    override fun addChangeListener(onChange: (old: T, new: T) -> Unit) = Unit

    override fun removeChangeListener(onChange: (old: T, new: T) -> Unit) = Unit

    override fun <R> map(transform: (T) -> R): Property<R> = immutablePropertyOf(transform(value))

    /**
     * Maps [that], but passes [value] instead of [this], because value may not change
     */
    override fun <U, R> mapWith(that: Property<U>, transform: (T, U) -> R): Property<R> =
            MappedProperty(that, { transform(value, it) })

}
