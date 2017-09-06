package net.aquadc.properties

import net.aquadc.properties.internal.ConcurrentBiMappedCachedProperty
import net.aquadc.properties.internal.MappedProperty

interface Property<out T> {

    val value: T

    fun addChangeListener(onChange: (old: T, new: T) -> Unit)
    fun removeChangeListener(onChange: (old: T, new: T) -> Unit)

    fun <R> map(transform: (T) -> R): Property<R> =
            MappedProperty<T, R>(this, transform)

    fun <U, R> mapWith(that: Property<U>, transform: (T, U) -> R): Property<R> =
            ConcurrentBiMappedCachedProperty(this, that, transform)

}
