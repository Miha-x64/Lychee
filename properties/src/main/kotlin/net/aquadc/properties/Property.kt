package net.aquadc.properties

interface Property<out T> {

    fun getValue(): T
    val mayChange: Boolean
    val isConcurrent: Boolean

    fun addChangeListener(onChange: ChangeListener<T>)
    fun removeChangeListener(onChange: ChangeListener<T>)

}
