package net.aquadc.properties.persistence

import net.aquadc.persistence.stream.BetterDataOutput
import net.aquadc.persistence.stream.write
import net.aquadc.persistence.type.DataType
import net.aquadc.properties.MutableProperty

/**
 * Writes data from properties into [output].
 */
class PropertyWriter<T>(
        private val kind: BetterDataOutput<T>,
        private val output: T
) : PropertyIo {

    override fun <T> DataType<T>.invoke(prop: MutableProperty<T>) {
        write(kind, output, prop.value)
    }

}
