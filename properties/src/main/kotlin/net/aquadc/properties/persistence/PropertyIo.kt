@file:Suppress("NOTHING_TO_INLINE")
package net.aquadc.properties.persistence

import net.aquadc.properties.MutableProperty
import java.util.*


interface PropertyIo {
    fun bool(prop: MutableProperty<Boolean>)
    fun byte(prop: MutableProperty<Byte>)
    // Short is a rare type, not supported
    // Property<Char> does not look very useful, not supported
    fun int(prop: MutableProperty<Int>)
    fun long(prop: MutableProperty<Long>)
    fun float(prop: MutableProperty<Float>)
    fun double(prop: MutableProperty<Double>)

    fun bytes(prop: MutableProperty<ByteArray>)
    // ShortArray is a rare type, not supported
    fun chars(prop: MutableProperty<CharArray>)
    fun ints(prop: MutableProperty<IntArray>)
    fun longs(prop: MutableProperty<LongArray>)
    fun floats(prop: MutableProperty<FloatArray>)
    fun doubles(prop: MutableProperty<DoubleArray>)

    fun string(prop: MutableProperty<String>)
    fun stringList(prop: MutableProperty<List<String>>)

    fun <E : Enum<E>> enum(prop: MutableProperty<E>, type: Class<E>)
    fun <E : Enum<E>> enumSet(prop: MutableProperty<Set<E>>, type: Class<E>)
}


@JvmName("bool") inline infix fun PropertyIo.x(prop: MutableProperty<Boolean>) = bool(prop)
@JvmName("byte") inline infix fun PropertyIo.x(prop: MutableProperty<Byte>) = byte(prop)
@JvmName("int") inline infix fun PropertyIo.x(prop: MutableProperty<Int>) = int(prop)
@JvmName("long") inline infix fun PropertyIo.x(prop: MutableProperty<Long>) = long(prop)
@JvmName("float") inline infix fun PropertyIo.x(prop: MutableProperty<Float>) = float(prop)
@JvmName("double") inline infix fun PropertyIo.x(prop: MutableProperty<Double>) = double(prop)

@JvmName("bytes") inline infix fun PropertyIo.x(prop: MutableProperty<ByteArray>) = bytes(prop)
@JvmName("chars") inline infix fun PropertyIo.x(prop: MutableProperty<CharArray>) = chars(prop)
@JvmName("ints") inline infix fun PropertyIo.x(prop: MutableProperty<IntArray>) = ints(prop)
@JvmName("longs") inline infix fun PropertyIo.x(prop: MutableProperty<LongArray>) = longs(prop)
@JvmName("floats") inline infix fun PropertyIo.x(prop: MutableProperty<FloatArray>) = floats(prop)
@JvmName("doubles") inline infix fun PropertyIo.x(prop: MutableProperty<DoubleArray>) = doubles(prop)

@JvmName("string") inline infix fun PropertyIo.x(prop: MutableProperty<String>) = string(prop)
@JvmName("stringList") inline infix fun PropertyIo.x(prop: MutableProperty<List<String>>) = stringList(prop)

@JvmName("enum") inline infix fun <reified E : Enum<E>> PropertyIo.x(prop: MutableProperty<E>) =
        enum(prop, E::class.java)
@JvmName("enumSet") inline infix fun <reified E : Enum<E>> PropertyIo.x(prop: MutableProperty<Set<E>>) =
        enumSet(prop, E::class.java)

@Suppress("UNCHECKED_CAST") // we'll read it as Set and write as EnumSet
@JvmName("realEnumSet") inline infix fun <reified E : Enum<E>> PropertyIo.x(prop: MutableProperty<EnumSet<E>>) =
        enumSet(prop as MutableProperty<Set<E>>, E::class.java)
