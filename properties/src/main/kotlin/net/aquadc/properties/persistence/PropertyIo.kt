@file:Suppress("NOTHING_TO_INLINE")
package net.aquadc.properties.persistence

import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.bool
import net.aquadc.persistence.type.i8
import net.aquadc.persistence.type.byteArray
import net.aquadc.persistence.type.collection
import net.aquadc.persistence.type.f64
import net.aquadc.persistence.type.f32
import net.aquadc.persistence.type.i32
import net.aquadc.persistence.type.i64
import net.aquadc.persistence.type.i16
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
@JvmName("i8") inline infix fun PropertyIo.x(prop: MutableProperty<Byte>): Unit =
        i8.invoke(prop)

/** Reads or writes a [Short] value */
@JvmName("i16") inline infix fun PropertyIo.x(prop: MutableProperty<Short>): Unit =
        i16.invoke(prop)

/** Reads or writes an [Int] value */
@JvmName("i32") inline infix fun PropertyIo.x(prop: MutableProperty<Int>): Unit =
        i32.invoke(prop)

/** Reads or writes a [Long] value */
@JvmName("i64") inline infix fun PropertyIo.x(prop: MutableProperty<Long>): Unit =
        i64.invoke(prop)

/** Reads or writes a [Float] value */
@JvmName("f32") inline infix fun PropertyIo.x(prop: MutableProperty<Float>): Unit =
        f32.invoke(prop)

/** Reads or writes a [Double] value */
@JvmName("f64") inline infix fun PropertyIo.x(prop: MutableProperty<Double>): Unit =
        f64.invoke(prop)

/** Reads or writes a [ByteArray] */
@JvmName("bytes") inline infix fun PropertyIo.x(prop: MutableProperty<ByteArray>): Unit =
        byteArray.invoke(prop)

/** Reads or writes a [String] */
@JvmName("string") inline infix fun PropertyIo.x(prop: MutableProperty<String>): Unit =
        string.invoke(prop)

private val StringList = collection(string)
/** Reads or writes a [List] of [String]s */
@JvmName("stringList") infix fun PropertyIo.x(prop: MutableProperty<List<String>>): Unit = StringList(prop)
