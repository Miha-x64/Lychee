package net.aquadc.properties.persistence

import net.aquadc.persistence.type.Converter
import net.aquadc.persistence.type.DataIoConverter
import net.aquadc.persistence.stream.CleverDataOutput
import net.aquadc.properties.MutableProperty
import java.lang.Double.doubleToLongBits
import java.lang.Float.floatToIntBits

/**
 * Writes data from properties into [output].
 */
class PropertyWriter(
        private val output: CleverDataOutput
) : PropertyIo {

    override fun <T> Converter<T>.invoke(prop: MutableProperty<T>) {
        (this as DataIoConverter<T>).write(output, prop.value)
    }

    override fun chars(prop: MutableProperty<CharArray>) {
        output.writeCharArray(prop.value)
    }
    override fun ints(prop: MutableProperty<IntArray>) {
        output.writeIntArray(prop.value)
    }
    override fun longs(prop: MutableProperty<LongArray>) {
        output.writeLongArray(prop.value)
    }
    override fun floats(prop: MutableProperty<FloatArray>) {
        output.writeFloatArray(prop.value)
    }
    override fun doubles(prop: MutableProperty<DoubleArray>) {
        output.writeDoubleArray(prop.value)
    }

    override fun stringList(prop: MutableProperty<List<String>>) {
        output.writeStringList(prop.value)
    }

    override fun <E : Enum<E>> enum(prop: MutableProperty<E>, type: Class<E>) {
        output.writeString(prop.value.name)
    }
    override fun <E : Enum<E>> enumSet(prop: MutableProperty<Set<E>>, type: Class<E>) {
        output.writeEnumSet(prop.value)
    }

}

internal fun CleverDataOutput.writeCharArray(value: CharArray) {
    writeInt(value.size)
    value.forEach { writeShort(it.toInt()) }
}
internal fun CleverDataOutput.writeIntArray(value: IntArray) {
    writeInt(value.size)
    value.forEach(::writeInt)
}
internal fun CleverDataOutput.writeLongArray(value: LongArray) {
    writeInt(value.size)
    value.forEach(::writeLong)
}
internal fun CleverDataOutput.writeFloatArray(value: FloatArray) {
    writeInt(value.size)
    value.forEach { writeInt(floatToIntBits(it)) }
}
internal fun CleverDataOutput.writeDoubleArray(value: DoubleArray) {
    writeInt(value.size)
    value.forEach { writeLong(doubleToLongBits(it)) }
}
internal fun CleverDataOutput.writeStringList(value: List<String>) {
    writeInt(value.size)
    for (i in 0 until value.size) writeString(value[i])
}
internal fun <E : Enum<E>> CleverDataOutput.writeEnumSet(value: Set<E>) {
    writeInt(value.size)
    value.forEach { writeString(it.name) }
}
