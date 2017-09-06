package net.aquadc.properties.internal

import net.aquadc.properties.Property
import net.aquadc.properties.immutablePropertyOf

class ImmutableReferenceProperty<out T>(
        override val value: T
) : Property<T> {

    override val mayChange: Boolean get() = false

    override fun addChangeListener(onChange: (old: T, new: T) -> Unit) = Unit

    override fun removeChangeListener(onChange: (old: T, new: T) -> Unit) = Unit

    /**
     * Maps value instead of property, because value may not change
     */
    override fun <R> map(transform: (T) -> R): Property<R> = immutablePropertyOf(transform(value))

    /**
     * Maps [that], but passes [value] instead of [this], because value may not change
     */
    override fun <U, R> mapWith(that: Property<U>, transform: (T, U) -> R): Property<R> =
            that.map { transform(value, it) }

}
