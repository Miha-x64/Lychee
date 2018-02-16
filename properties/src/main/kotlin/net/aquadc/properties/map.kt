package net.aquadc.properties

import net.aquadc.properties.internal.ConcBiMappedProperty
import net.aquadc.properties.internal.ConcMappedProperty
import net.aquadc.properties.internal.UnsBiMappedProperty
import net.aquadc.properties.internal.UnsMappedProperty

fun <T, R> Property<T>.map(transform: (T) -> R): Property<R> = when {
    this.mayChange -> {
        if (this.isConcurrent) ConcMappedProperty(this, transform)
        else UnsMappedProperty(this, transform)
    }
    else -> immutablePropertyOf(transform(value))
}

fun <T, U, R> Property<T>.mapWith(that: Property<U>, transform: (T, U) -> R): Property<R> = when {
    this.mayChange && that.mayChange -> {
        if (this.isConcurrent && that.isConcurrent) ConcBiMappedProperty(this, that, transform)
        else UnsBiMappedProperty(this, that, transform)
    }
    !this.mayChange -> {
        val thisValue = this.value
        that.map { transform(thisValue, it) }
    }
    !that.mayChange -> {
        val thatValue = that.value
        this.map { transform(it, thatValue) }
    }
    else -> throw AssertionError()
}
