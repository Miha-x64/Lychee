package net.aquadc.properties.persistence

import net.aquadc.properties.MutableProperty
import java.io.DataInput
import java.util.*

/**
 * Reads data from [input] into properties.
 */
class PropertyInput(
        private val input: DataInput
) : PropertyIo {

    override fun bool(prop: MutableProperty<Boolean>) {
        prop.value = input.readBoolean()
    }
    override fun byte(prop: MutableProperty<Byte>) {
        prop.value = input.readByte()
    }
    override fun short(prop: MutableProperty<Short>) {
        prop.value = input.readShort()
    }
    override fun char(prop: MutableProperty<Char>) {
        prop.value = input.readChar()
    }
    override fun int(prop: MutableProperty<Int>) {
        prop.value = input.readInt()
    }
    override fun long(prop: MutableProperty<Long>) {
        prop.value = input.readLong()
    }
    override fun float(prop: MutableProperty<Float>) {
        prop.value = input.readFloat()
    }
    override fun double(prop: MutableProperty<Double>) {
        prop.value = input.readDouble()
    }

    override fun bytes(prop: MutableProperty<ByteArray>) {
        prop.value = ByteArray(input.readInt()).also(input::readFully)
    }
    override fun shorts(prop: MutableProperty<ShortArray>) {
        prop.value = ShortArray(input.readInt()) { input.readShort() }
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

    override fun string(prop: MutableProperty<String>) {
        prop.value = input.readUTF()
    }
    override fun stringArr(prop: MutableProperty<Array<String>>) {
        prop.value = Array(input.readInt()) { input.readUTF() }
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
