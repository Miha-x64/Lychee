package net.aquadc.properties

import net.aquadc.properties.internal.ConcurrentBiMappedCachedReferenceProperty
import net.aquadc.properties.internal.ConcurrentMappedReferenceProperty

interface Property<out T> {

    val value: T
    val mayChange: Boolean

    fun addChangeListener(onChange: (old: T, new: T) -> Unit)
    fun removeChangeListener(onChange: (old: T, new: T) -> Unit)

    fun <R> map(transform: (T) -> R): Property<R> =
            ConcurrentMappedReferenceProperty<T, R>(this, transform)

    fun <U, R> mapWith(that: Property<U>, transform: (T, U) -> R): Property<R> = if (that.mayChange) {
        ConcurrentBiMappedCachedReferenceProperty(this, that, transform)
    } else {
        val thatValue = that.value
        map { transform(it, thatValue) }
    }

}
