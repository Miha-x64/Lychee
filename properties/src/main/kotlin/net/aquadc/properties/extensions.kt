@file:JvmName("Properties")
package net.aquadc.properties

import net.aquadc.properties.executor.InPlaceWorker
import net.aquadc.properties.executor.Worker
import net.aquadc.properties.internal.BiMappedProperty
import net.aquadc.properties.internal.ImmutableReferenceProperty
import net.aquadc.properties.internal.MappedProperty

/**
 * Returns new property with [transform]ed value.
 */
fun <T, R> Property<T>.map(transform: (T) -> R): Property<R> = when {
    this.mayChange -> MappedProperty(this, transform, InPlaceWorker)
    else -> immutablePropertyOf(transform(value))
}

/**
 * Returns new property with [transform]ed value.
 * Calling [transform] from [worker] thread is not guaranteed:
 * it will be called in-place if there's no pre-mapped value.
 */
fun <T, R> Property<T>.mapOn(worker: Worker, transform: (T) -> R): Property<R> = when {
    this.mayChange -> MappedProperty(this, transform, worker)
    else -> immutablePropertyOf(transform(value))
}

fun <T, U, R> Property<T>.mapWith(that: Property<U>, transform: (T, U) -> R): Property<R> = when {
    this.mayChange && that.mayChange -> {
        BiMappedProperty(this, that, transform)
    }
    !this.mayChange -> {
        val thisValue = this.value
        that.map { transform(thisValue, it) }
    }
    !that.mayChange -> {
        val thatValue = that.value
        this.map { transform(it, thatValue) }
    }
    else -> ImmutableReferenceProperty(transform(this.value, that.value))
}

/**
 * Calls [func] for each [Property.value] including initial.
 */
fun <T> Property<T>.onEach(func: (T) -> Unit) {
    addChangeListener { _, new -> func(new) }
    // *
    func(value)

    /*
    What's better — call func first or add listener first?
    a) if we call function first, in (*) place value may change and we won't see new one
    b) if we add listener first, in (*) place value may change and we'll call func after that, in wrong order
     */
}