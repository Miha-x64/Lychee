@file:Suppress("NOTHING_TO_INLINE")
package net.aquadc.properties.persistence

import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.bool
import net.aquadc.persistence.type.byte
import net.aquadc.persistence.type.byteArray
import net.aquadc.persistence.type.collection
import net.aquadc.persistence.type.double
import net.aquadc.persistence.type.float
import net.aquadc.persistence.type.int
import net.aquadc.persistence.type.long
import net.aquadc.persistence.type.short
import net.aquadc.persistence.type.string
import net.aquadc.properties.MutableProperty

/**
 * Describes an object which can either save or restore some data.
 * Similar to [java.io.DataInput], [java.io.DataOutput] or `android.os.Parcel`,
 * but uses the same API for reading and writing.
 */
interface PropertyIo {

    /**
     * Reads or writes a value of [this] type [T] from/to [prop].
     */
    operator fun <T> DataType<T>.invoke(prop: MutableProperty<T>)

}

// TODO: maybe rename `x`

/** Reads or writes a [Boolean] value */
@JvmName("bool") inline infix fun PropertyIo.x(prop: MutableProperty<Boolean>): Unit =
        bool.invoke(prop)

/** Reads or writes a [Byte] value */
@JvmName("byte") inline infix fun PropertyIo.x(prop: MutableProperty<Byte>): Unit =
        byte.invoke(prop)

/** Reads or writes a [Short] value */
@JvmName("short") inline infix fun PropertyIo.x(prop: MutableProperty<Short>): Unit =
        short.invoke(prop)

/** Reads or writes an [Int] value */
@JvmName("int") inline infix fun PropertyIo.x(prop: MutableProperty<Int>): Unit =
        int.invoke(prop)

/** Reads or writes a [Long] value */
@JvmName("long") inline infix fun PropertyIo.x(prop: MutableProperty<Long>): Unit =
        long.invoke(prop)

/** Reads or writes a [Float] value */
@JvmName("float") inline infix fun PropertyIo.x(prop: MutableProperty<Float>): Unit =
        float.invoke(prop)

/** Reads or writes a [Double] value */
@JvmName("double") inline infix fun PropertyIo.x(prop: MutableProperty<Double>): Unit =
        double.invoke(prop)

/** Reads or writes a [ByteArray] */
@JvmName("bytes") inline infix fun PropertyIo.x(prop: MutableProperty<ByteArray>): Unit =
        byteArray.invoke(prop)

/** Reads or writes a [String] */
@JvmName("string") inline infix fun PropertyIo.x(prop: MutableProperty<String>): Unit =
        string.invoke(prop)

private val StringList = collection(string)
/** Reads or writes a [List] of [String]s */
@JvmName("stringList") infix fun PropertyIo.x(prop: MutableProperty<List<String>>): Unit = StringList(prop)
