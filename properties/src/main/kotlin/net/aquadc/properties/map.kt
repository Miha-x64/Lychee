package net.aquadc.properties

import net.aquadc.properties.internal.ConcurrentBiMappedCachedReferenceProperty
import net.aquadc.properties.internal.ConcurrentMappedReferenceProperty
import net.aquadc.properties.internal.UnsynchronizedBiMappedCachedReferenceProperty
import net.aquadc.properties.internal.UnsynchronizedMappedReferenceProperty

fun <T, R> Property<T>.map(transform: (T) -> R): Property<R> = when {
    this.mayChange -> {
        if (this.isConcurrent) ConcurrentMappedReferenceProperty(this, transform)
        else UnsynchronizedMappedReferenceProperty(this, transform)
    }
    else -> immutablePropertyOf(transform(value))
}

fun <T, U, R> Property<T>.mapWith(that: Property<U>, transform: (T, U) -> R): Property<R> = when {
    this.mayChange && that.mayChange -> {
        if (this.isConcurrent && that.isConcurrent) ConcurrentBiMappedCachedReferenceProperty(this, that, transform)
        else UnsynchronizedBiMappedCachedReferenceProperty(this, that, transform)
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
