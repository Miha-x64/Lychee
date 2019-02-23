package net.aquadc.properties.persistence

import net.aquadc.persistence.stream.BetterDataOutput
import net.aquadc.persistence.stream.write
import net.aquadc.persistence.type.DataType
import net.aquadc.properties.MutableProperty
import java.lang.Double.doubleToLongBits
import java.lang.Float.floatToIntBits

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

internal fun <T> BetterDataOutput<T>.writeCharArray(output: T, value: CharArray) {
    writeInt(output, value.size)
    value.forEach { writeShort(output, it.toShort()) }
}
internal fun <T> BetterDataOutput<T>.writeIntArray(output: T, value: IntArray) {
    writeInt(output, value.size)
    value.forEach { writeInt(output, it) }
}
internal fun <T> BetterDataOutput<T>.writeLongArray(output: T, value: LongArray) {
    writeInt(output, value.size)
    value.forEach { writeLong(output, it) }
}
internal fun <T> BetterDataOutput<T>.writeFloatArray(output: T, value: FloatArray) {
    writeInt(output, value.size)
    value.forEach { writeInt(output, floatToIntBits(it)) }
}
internal fun <T> BetterDataOutput<T>.writeDoubleArray(output: T, value: DoubleArray) {
    writeInt(output, value.size)
    value.forEach { writeLong(output, doubleToLongBits(it)) }
}
internal fun <T> BetterDataOutput<T>.writeStringList(output: T, value: List<String>) {
    writeInt(output, value.size)
    for (i in value.indices) writeString(output, value[i])
}
internal fun <T, E : Enum<E>> BetterDataOutput<T>.writeEnumSet(output: T, value: Set<E>) {
    writeInt(output, value.size)
    value.forEach { writeString(output, it.name) }
}
