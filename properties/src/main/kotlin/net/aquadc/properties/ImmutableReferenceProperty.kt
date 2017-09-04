package net.aquadc.properties

class ImmutableReferenceProperty<out T>(
        override val value: T
) : Property<T> {

    override fun addChangeListener(onChange: (old: T, new: T) -> Unit) = Unit

    override fun removeChangeListener(onChange: (old: T, new: T) -> Unit) = Unit

}
