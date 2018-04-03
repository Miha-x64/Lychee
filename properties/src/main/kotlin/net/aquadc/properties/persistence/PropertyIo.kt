@file:Suppress("NOTHING_TO_INLINE")
package net.aquadc.properties.persistence

import net.aquadc.properties.MutableProperty
import java.util.*

/**
 * Describes an object which can either save or restore some data.
 * Similar to [java.io.DataInput], [java.io.DataOutput] or `android.os.Parcel`,
 * but uses the same API for reading and writing.
 */
interface PropertyIo {
    /** Reads or writes a [Boolean] value */
    fun bool(prop: MutableProperty<Boolean>)
    /** Reads or writes a [Byte] value */
    fun byte(prop: MutableProperty<Byte>)
    // Short is a rare type, not supported
    // Property<Char> does not look very useful, not supported
    /** Reads or writes an [Int] value */
    fun int(prop: MutableProperty<Int>)
    /** Reads or writes a [Long] value */
    fun long(prop: MutableProperty<Long>)
    /** Reads or writes a [Float] value */
    fun float(prop: MutableProperty<Float>)
    /** Reads or writes a [Double] value */
    fun double(prop: MutableProperty<Double>)

    /** Reads or writes a [ByteArray] */
    fun bytes(prop: MutableProperty<ByteArray>)
    // ShortArray is a rare type, not supported
    /** Reads or writes a [CharArray] */
    fun chars(prop: MutableProperty<CharArray>)
    /** Reads or writes an [IntArray] */
    fun ints(prop: MutableProperty<IntArray>)
    /** Reads or writes a [LongArray] */
    fun longs(prop: MutableProperty<LongArray>)
    /** Reads or writes a [FloatArray] */
    fun floats(prop: MutableProperty<FloatArray>)
    /** Reads or writes a [DoubleArray] */
    fun doubles(prop: MutableProperty<DoubleArray>)

    /** Reads or writes a [String] */
    fun string(prop: MutableProperty<String>)
    /** Reads or writes a [List] of [String]s */
    fun stringList(prop: MutableProperty<List<String>>)

    /** Reads or writes an [Enum] */
    fun <E : Enum<E>> enum(prop: MutableProperty<E>, type: Class<E>)
    /** Reads or writes a [Set] of [Enum] values */
    fun <E : Enum<E>> enumSet(prop: MutableProperty<Set<E>>, type: Class<E>)
}

/** Reads or writes a [Boolean] value */
@JvmName("bool") inline infix fun PropertyIo.x(prop: MutableProperty<Boolean>) = bool(prop)
/** Reads or writes a [Byte] value */
@JvmName("byte") inline infix fun PropertyIo.x(prop: MutableProperty<Byte>) = byte(prop)
/** Reads or writes an [Int] value */
@JvmName("int") inline infix fun PropertyIo.x(prop: MutableProperty<Int>) = int(prop)
/** Reads or writes a [Long] value */
@JvmName("long") inline infix fun PropertyIo.x(prop: MutableProperty<Long>) = long(prop)
/** Reads or writes a [Float] value */
@JvmName("float") inline infix fun PropertyIo.x(prop: MutableProperty<Float>) = float(prop)
/** Reads or writes a [Double] value */
@JvmName("double") inline infix fun PropertyIo.x(prop: MutableProperty<Double>) = double(prop)

/** Reads or writes a [ByteArray] */
@JvmName("bytes") inline infix fun PropertyIo.x(prop: MutableProperty<ByteArray>) = bytes(prop)
/** Reads or writes a [CharArray] */
@JvmName("chars") inline infix fun PropertyIo.x(prop: MutableProperty<CharArray>) = chars(prop)
/** Reads or writes an [IntArray] */
@JvmName("ints") inline infix fun PropertyIo.x(prop: MutableProperty<IntArray>) = ints(prop)
/** Reads or writes a [LongArray] */
@JvmName("longs") inline infix fun PropertyIo.x(prop: MutableProperty<LongArray>) = longs(prop)
/** Reads or writes a [FloatArray] */
@JvmName("floats") inline infix fun PropertyIo.x(prop: MutableProperty<FloatArray>) = floats(prop)
/** Reads or writes a [DoubleArray] */
@JvmName("doubles") inline infix fun PropertyIo.x(prop: MutableProperty<DoubleArray>) = doubles(prop)

/** Reads or writes a [String] */
@JvmName("string") inline infix fun PropertyIo.x(prop: MutableProperty<String>) = string(prop)
/** Reads or writes a [List] of [String]s */
@JvmName("stringList") inline infix fun PropertyIo.x(prop: MutableProperty<List<String>>) = stringList(prop)

/** Reads or writes an [Enum] */
@JvmName("enum") inline infix fun <reified E : Enum<E>> PropertyIo.x(prop: MutableProperty<E>) =
        enum(prop, E::class.java)
/** Reads or writes a [Set] of [Enum] values */
@JvmName("enumSet") inline infix fun <reified E : Enum<E>> PropertyIo.x(prop: MutableProperty<Set<E>>) =
        enumSet(prop, E::class.java)

@Suppress("UNCHECKED_CAST") // we'll read it as Set and write as EnumSet
/** Reads or writes a [Set] of [Enum] values */
@JvmName("realEnumSet") inline infix fun <reified E : Enum<E>> PropertyIo.x(prop: MutableProperty<EnumSet<E>>) =
        enumSet(prop as MutableProperty<Set<E>>, E::class.java)
