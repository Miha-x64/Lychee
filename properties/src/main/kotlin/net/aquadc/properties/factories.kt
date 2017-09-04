package net.aquadc.properties

import net.aquadc.properties.internal.ConcurrentMutableReferenceProperty
import net.aquadc.properties.internal.ImmutableReferenceProperty

fun <T> mutablePropertyOf(value: T): MutableProperty<T> =
        ConcurrentMutableReferenceProperty(value)

private val immutableTrue = ImmutableReferenceProperty(true)
private val immutableFalse = ImmutableReferenceProperty(false)

@Suppress("UNCHECKED_CAST") // it's safe, I PROVE IT
fun <T> immutablePropertyOf(value: T): Property<T> = when (value) {
    true -> immutableTrue as Property<T>
    false -> immutableFalse as Property<T>
    else -> ImmutableReferenceProperty(value)
}
