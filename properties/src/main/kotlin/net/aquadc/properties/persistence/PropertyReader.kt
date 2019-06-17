package net.aquadc.properties.persistence

import net.aquadc.persistence.stream.BetterDataInput
import net.aquadc.persistence.stream.read
import net.aquadc.persistence.type.DataType
import net.aquadc.properties.MutableProperty

/**
 * Reads data from [input] into properties.
 */
class PropertyReader<T>(
        private val kind: BetterDataInput<T>,
        private val input: T
) : PropertyIo {

    override fun <T> DataType<T>.invoke(prop: MutableProperty<T>) {
        prop.value = kind.read(input, this)
    }

}
