@file:Suppress("NOTHING_TO_INLINE")
package net.aquadc.properties.persistence

import net.aquadc.persistence.converter.Converter
import net.aquadc.properties.MutableProperty
import java.util.*

/**
 * Describes an object which can either save or restore some data.
 * Similar to [java.io.DataInput], [java.io.DataOutput] or `android.os.Parcel`,
 * but uses the same API for reading and writing.
 */
interface PropertyIo {

    /**
     * Reads or writes a value of type [T] from/to [prop] using [this] converter.
     */
    operator fun <T> Converter<T>.invoke(prop: MutableProperty<T>)

    /** Reads or writes a [CharArray] */
    @Deprecated("not sure whether it is useful")
    fun chars(prop: MutableProperty<CharArray>)

    /** Reads or writes an [IntArray] */
    @Deprecated("not sure whether it is useful")
    fun ints(prop: MutableProperty<IntArray>)

    /** Reads or writes a [LongArray] */
    @Deprecated("not sure whether it is useful")
    fun longs(prop: MutableProperty<LongArray>)

    /** Reads or writes a [FloatArray] */
    @Deprecated("not sure whether it is useful")
    fun floats(prop: MutableProperty<FloatArray>)

    /** Reads or writes a [DoubleArray] */
    @Deprecated("not sure whether it is useful")
    fun doubles(prop: MutableProperty<DoubleArray>)

    /** Reads or writes a [List] of [String]s */
    @Deprecated("should be implemented in a converter")
    fun stringList(prop: MutableProperty<List<String>>)

    /** Reads or writes an [Enum] */
    @Deprecated("should create custom converter instance")
    fun <E : Enum<E>> enum(prop: MutableProperty<E>, type: Class<E>)

    /** Reads or writes a [Set] of [Enum] values */
    @Deprecated("should be implemented on top of Converters")
    fun <E : Enum<E>> enumSet(prop: MutableProperty<Set<E>>, type: Class<E>)
}

/** Reads or writes a [Boolean] value */
@JvmName("bool") inline infix fun PropertyIo.x(prop: MutableProperty<Boolean>) =
        net.aquadc.persistence.converter.bool.invoke(prop)

/** Reads or writes a [Byte] value */
@JvmName("byte") inline infix fun PropertyIo.x(prop: MutableProperty<Byte>) =
        net.aquadc.persistence.converter.byte.invoke(prop)

/** Reads or writes a [Short] value */
@JvmName("short") inline infix fun PropertyIo.x(prop: MutableProperty<Short>) =
        net.aquadc.persistence.converter.short.invoke(prop)

/** Reads or writes an [Int] value */
@JvmName("int") inline infix fun PropertyIo.x(prop: MutableProperty<Int>) =
        net.aquadc.persistence.converter.int.invoke(prop)

/** Reads or writes a [Long] value */
@JvmName("long") inline infix fun PropertyIo.x(prop: MutableProperty<Long>) =
        net.aquadc.persistence.converter.long.invoke(prop)

/** Reads or writes a [Float] value */
@JvmName("float") inline infix fun PropertyIo.x(prop: MutableProperty<Float>) =
        net.aquadc.persistence.converter.float.invoke(prop)

/** Reads or writes a [Double] value */
@JvmName("double") inline infix fun PropertyIo.x(prop: MutableProperty<Double>) =
        net.aquadc.persistence.converter.double.invoke(prop)

/** Reads or writes a [ByteArray] */
@JvmName("bytes") inline infix fun PropertyIo.x(prop: MutableProperty<ByteArray>) =
        net.aquadc.persistence.converter.bytes.invoke(prop)

/** Reads or writes a [CharArray] */ @Deprecated("not sure whether it is useful")
@JvmName("chars") inline infix fun PropertyIo.x(prop: MutableProperty<CharArray>) = chars(prop)

/** Reads or writes an [IntArray] */ @Deprecated("not sure whether it is useful")
@JvmName("ints") inline infix fun PropertyIo.x(prop: MutableProperty<IntArray>) = ints(prop)

/** Reads or writes a [LongArray] */ @Deprecated("not sure whether it is useful")
@JvmName("longs") inline infix fun PropertyIo.x(prop: MutableProperty<LongArray>) = longs(prop)

/** Reads or writes a [FloatArray] */  @Deprecated("not sure whether it is useful")
@JvmName("floats") inline infix fun PropertyIo.x(prop: MutableProperty<FloatArray>) = floats(prop)

/** Reads or writes a [DoubleArray] */ @Deprecated("not sure whether it is useful")
@JvmName("doubles") inline infix fun PropertyIo.x(prop: MutableProperty<DoubleArray>) = doubles(prop)

/** Reads or writes a [String] */
@JvmName("string") inline infix fun PropertyIo.x(prop: MutableProperty<String>) =
        net.aquadc.persistence.converter.string.invoke(prop)

/** Reads or writes a [List] of [String]s */ // todo: implement String[], List<String> in common converters
@JvmName("stringList") inline infix fun PropertyIo.x(prop: MutableProperty<List<String>>) = stringList(prop)

/** Reads or writes an [Enum] */
@Deprecated("internally this uses reflection. Should use own converter instead",
        ReplaceWith("net.aquadc.persistence.converter.enum(E.values())"))
@JvmName("enum") inline infix fun <reified E : Enum<E>> PropertyIo.x(prop: MutableProperty<E>) =
        enum(prop, E::class.java)

/** Reads or writes a [Set] of [Enum] values */ // todo: implement EnumSet converter
@JvmName("enumSet") inline infix fun <reified E : Enum<E>> PropertyIo.x(prop: MutableProperty<Set<E>>) =
        enumSet(prop, E::class.java)

@Suppress("UNCHECKED_CAST") // we'll read it as Set and write as EnumSet
/** Reads or writes a [Set] of [Enum] values */
@JvmName("realEnumSet") inline infix fun <reified E : Enum<E>> PropertyIo.x(prop: MutableProperty<EnumSet<E>>) =
        enumSet(prop as MutableProperty<Set<E>>, E::class.java)
