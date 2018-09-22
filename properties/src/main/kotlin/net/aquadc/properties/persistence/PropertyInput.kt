package net.aquadc.properties.persistence

import net.aquadc.persistence.converter.Converter
import net.aquadc.persistence.converter.DataIoConverter
import net.aquadc.properties.MutableProperty
import java.io.DataInput
import java.util.*

/**
 * Reads data from [input] into properties.
 */
class PropertyInput(
        private val input: DataInput
) : PropertyIo {

    override fun <T> Converter<T>.invoke(prop: MutableProperty<T>) {
        prop.value = (this as DataIoConverter<T>).read(input)
    }

    override fun chars(prop: MutableProperty<CharArray>) {
        prop.value = CharArray(input.readInt()) { input.readChar() }
    }
    override fun ints(prop: MutableProperty<IntArray>) {
        prop.value = IntArray(input.readInt()) { input.readInt() }
    }
    override fun longs(prop: MutableProperty<LongArray>) {
        prop.value = LongArray(input.readInt()) { input.readLong() }
    }
    override fun floats(prop: MutableProperty<FloatArray>) {
        prop.value = FloatArray(input.readInt()) { input.readFloat() }
    }
    override fun doubles(prop: MutableProperty<DoubleArray>) {
        prop.value = DoubleArray(input.readInt()) { input.readDouble() }
    }

    override fun stringList(prop: MutableProperty<List<String>>) {
        prop.value = List(input.readInt()) { input.readUTF() }
    }

    override fun <E : Enum<E>> enum(prop: MutableProperty<E>, type: Class<E>) {
        prop.value = java.lang.Enum.valueOf(type, input.readUTF())
    }
    override fun <E : Enum<E>> enumSet(prop: MutableProperty<Set<E>>, type: Class<E>) {
        prop.value = EnumSet.noneOf(type).also { set ->
            repeat(input.readInt()) {
                set.add(java.lang.Enum.valueOf(type, input.readUTF()))
            }
        }
    }

}
