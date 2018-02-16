package net.aquadc.properties.internal

import net.aquadc.properties.Property

/**
 * Base class for concurrent properties.
 * I don't like implementation inheritance, but it is more lightweight than composition.
 */
abstract class BaseConcProperty<out T> : Property<T> {

    final override val mayChange: Boolean
        get() = true

    final override val isConcurrent: Boolean
        get() = true

}
