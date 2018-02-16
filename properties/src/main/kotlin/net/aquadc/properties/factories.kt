package net.aquadc.properties

import net.aquadc.properties.internal.ConcMutableProperty
import net.aquadc.properties.internal.ImmutableReferenceProperty
import net.aquadc.properties.internal.UnsMutableProperty

fun <T> mutablePropertyOf(value: T, concurrent: Boolean): MutableProperty<T> =
        if (concurrent) ConcMutableProperty(value)
        else UnsMutableProperty(value)

fun <T> concurrentMutablePropertyOf(value: T): MutableProperty<T> =
        ConcMutableProperty(value)

fun <T> unsynchronizedMutablePropertyOf(value: T): MutableProperty<T> =
        UnsMutableProperty(value)

private val immutableTrue = ImmutableReferenceProperty(true)
private val immutableFalse = ImmutableReferenceProperty(false)

@Suppress("UNCHECKED_CAST") // it's safe, I PROVE IT
fun <T> immutablePropertyOf(value: T): Property<T> = when (value) {
    true -> immutableTrue as Property<T>
    false -> immutableFalse as Property<T>
    else -> ImmutableReferenceProperty(value)
}
