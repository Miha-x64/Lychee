package net.aquadc.properties.executor

import net.aquadc.properties.ChangeListener

internal class MappedValueProxy<T, U>(
        private val mapOn: Worker,
        private val map: (T) -> U,
        private val consumer: (U) -> Unit
) : ChangeListener<T> {

    override fun invoke(old: T, new: T) {
        mapOn.map(new, map, consumer)
    }

}
