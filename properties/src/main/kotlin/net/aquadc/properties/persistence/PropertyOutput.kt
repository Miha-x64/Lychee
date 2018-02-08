package net.aquadc.properties.persistence

import net.aquadc.properties.MutableProperty
import java.io.DataOutput

/**
 * Writes data from properties into [output].
 */
class PropertyOutput(
        private val output: DataOutput
) : PropertyIo {

    override fun bool(prop: MutableProperty<Boolean>) {
        output.writeBoolean(prop.value)
    }
    override fun byte(prop: MutableProperty<Byte>) {
        output.writeByte(prop.value.toInt())
    }
    override fun short(prop: MutableProperty<Short>) {
        output.writeShort(prop.value.toInt())
    }
    override fun char(prop: MutableProperty<Char>) {
        output.writeChar(prop.value.toInt())
    }
    override fun int(prop: MutableProperty<Int>) {
        output.writeInt(prop.value)
    }
    override fun long(prop: MutableProperty<Long>) {
        output.writeLong(prop.value)
    }
    override fun float(prop: MutableProperty<Float>) {
        output.writeFloat(prop.value)
    }
    override fun double(prop: MutableProperty<Double>) {
        output.writeDouble(prop.value)
    }

    override fun bytes(prop: MutableProperty<ByteArray>) {
        val value = prop.value
        output.writeInt(value.size)
        output.write(value)
    }
    override fun shorts(prop: MutableProperty<ShortArray>) {
        val value = prop.value
        output.writeInt(value.size)
        value.forEach { output.writeShort(it.toInt()) }
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

    override fun string(prop: MutableProperty<String>) {
        output.writeUTF(prop.value)
    }
    override fun stringArr(prop: MutableProperty<Array<String>>) {
        val value = prop.value
        output.writeInt(value.size)
        value.forEach(output::writeUTF)
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
