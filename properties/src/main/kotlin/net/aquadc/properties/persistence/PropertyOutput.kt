package net.aquadc.properties.persistence

import net.aquadc.persistence.converter.Converter
import net.aquadc.persistence.converter.DataIoConverter
import net.aquadc.properties.MutableProperty
import java.io.DataOutput

/**
 * Writes data from properties into [output].
 */
class PropertyOutput(
        private val output: DataOutput
) : PropertyIo {

    override fun <T> Converter<T>.invoke(prop: MutableProperty<T>) {
        (this as DataIoConverter<T>).write(output, prop.value)
    }

    override fun chars(prop: MutableProperty<CharArray>) {
        val value = prop.value
        output.writeInt(value.size)
        value.forEach { output.writeChar(it.toInt()) }
    }
    override fun ints(prop: MutableProperty<IntArray>) {
        val value = prop.value
        output.writeInt(value.size)
        value.forEach(output::writeInt)
    }
    override fun longs(prop: MutableProperty<LongArray>) {
        val value = prop.value
        output.writeInt(value.size)
        value.forEach(output::writeLong)
    }
    override fun floats(prop: MutableProperty<FloatArray>) {
        val value = prop.value
        output.writeInt(value.size)
        value.forEach(output::writeFloat)
    }
    override fun doubles(prop: MutableProperty<DoubleArray>) {
        val value = prop.value
        output.writeInt(value.size)
        value.forEach(output::writeDouble)
    }

    override fun stringList(prop: MutableProperty<List<String>>) {
        val value = prop.value
        output.writeInt(value.size)
        for (i in 0 until value.size) output.writeUTF(value[i])
    }

    override fun <E : Enum<E>> enum(prop: MutableProperty<E>, type: Class<E>) {
        output.writeUTF(prop.value.name)
    }
    override fun <E : Enum<E>> enumSet(prop: MutableProperty<Set<E>>, type: Class<E>) {
        val value = prop.value
        output.writeInt(value.size)
        value.forEach { output.writeUTF(it.name) }
    }

}
