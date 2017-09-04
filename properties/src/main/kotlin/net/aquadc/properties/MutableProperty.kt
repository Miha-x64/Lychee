package net.aquadc.properties

interface MutableProperty<T> : Property<T> {

    override var value: T

}
