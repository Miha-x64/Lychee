package net.aquadc.properties

fun <T> mutablePropertyOf(value: T): MutableProperty<T> =
        ConcurrentMutableReferenceProperty(value)

fun <T> immutablePropertyOf(value: T): Property<T> =
        ImmutableReferenceProperty(value)
