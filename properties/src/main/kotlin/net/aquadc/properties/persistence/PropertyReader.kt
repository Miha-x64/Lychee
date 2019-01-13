package net.aquadc.properties.persistence

import net.aquadc.persistence.stream.BetterDataInput
import net.aquadc.persistence.stream.read
import net.aquadc.persistence.type.DataType
import net.aquadc.properties.MutableProperty
import java.lang.Double.longBitsToDouble
import java.lang.Float.intBitsToFloat
import java.util.EnumSet

/**
 * Reads data from [input] into properties.
 */
class PropertyReader<T>(
        private val kind: BetterDataInput<T>,
        private val input: T
) : PropertyIo {

    override fun <T> DataType<T>.invoke(prop: MutableProperty<T>) {
        prop.value = read(kind, input)
    }

    override fun chars(prop: MutableProperty<CharArray>) {
        prop.value = CharArray(kind.readInt(input)) { kind.readShort(input).toChar() }
    }
    override fun ints(prop: MutableProperty<IntArray>) {
        prop.value = IntArray(kind.readInt(input)) { kind.readInt(input) }
    }
    override fun longs(prop: MutableProperty<LongArray>) {
        prop.value = LongArray(kind.readInt(input)) { kind.readLong(input) }
    }
    override fun floats(prop: MutableProperty<FloatArray>) {
        prop.value = FloatArray(kind.readInt(input)) { intBitsToFloat(kind.readInt(input)) }
    }
    override fun doubles(prop: MutableProperty<DoubleArray>) {
        prop.value = DoubleArray(kind.readInt(input)) { longBitsToDouble(kind.readLong(input)) }
    }

    override fun stringList(prop: MutableProperty<List<String>>) {
        prop.value = List(kind.readInt(input)) { kind.readString(input)!! }
    }

    override fun <E : Enum<E>> enum(prop: MutableProperty<E>, type: Class<E>) {
        prop.value = java.lang.Enum.valueOf(type, kind.readString(input)!!)
    }
    override fun <E : Enum<E>> enumSet(prop: MutableProperty<Set<E>>, type: Class<E>) {
        prop.value = EnumSet.noneOf(type).also { set ->
            repeat(kind.readInt(input)) {
                set.add(java.lang.Enum.valueOf(type, kind.readString(input)!!))
            }
        }
    }

}
