package net.aquadc.properties.persistence

import net.aquadc.persistence.converter.Converter
import net.aquadc.persistence.converter.DataIoConverter
import net.aquadc.persistence.stream.CleverDataInput
import net.aquadc.properties.MutableProperty
import java.lang.Double.longBitsToDouble
import java.lang.Float.intBitsToFloat
import java.util.*

/**
 * Reads data from [input] into properties.
 */
class PropertyReader(
        private val input: CleverDataInput
) : PropertyIo {

    override fun <T> Converter<T>.invoke(prop: MutableProperty<T>) {
        prop.value = (this as DataIoConverter<T>).read(input)
    }

    override fun chars(prop: MutableProperty<CharArray>) {
        prop.value = CharArray(input.readInt()) { input.readShort().toChar() }
    }
    override fun ints(prop: MutableProperty<IntArray>) {
        prop.value = IntArray(input.readInt()) { input.readInt() }
    }
    override fun longs(prop: MutableProperty<LongArray>) {
        prop.value = LongArray(input.readInt()) { input.readLong() }
    }
    override fun floats(prop: MutableProperty<FloatArray>) {
        prop.value = FloatArray(input.readInt()) { intBitsToFloat(input.readInt()) }
    }
    override fun doubles(prop: MutableProperty<DoubleArray>) {
        prop.value = DoubleArray(input.readInt()) { longBitsToDouble(input.readLong()) }
    }

    override fun stringList(prop: MutableProperty<List<String>>) {
        prop.value = List(input.readInt()) { input.readString()!! }
    }

    override fun <E : Enum<E>> enum(prop: MutableProperty<E>, type: Class<E>) {
        prop.value = java.lang.Enum.valueOf(type, input.readString()!!)
    }
    override fun <E : Enum<E>> enumSet(prop: MutableProperty<Set<E>>, type: Class<E>) {
        prop.value = EnumSet.noneOf(type).also { set ->
            repeat(input.readInt()) {
                set.add(java.lang.Enum.valueOf(type, input.readString()!!))
            }
        }
    }

}
