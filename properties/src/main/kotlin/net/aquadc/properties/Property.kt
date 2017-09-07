package net.aquadc.properties

interface Property<out T> {

    val value: T
    val mayChange: Boolean
    val isConcurrent: Boolean

    fun addChangeListener(onChange: (old: T, new: T) -> Unit)
    fun removeChangeListener(onChange: (old: T, new: T) -> Unit)

}
