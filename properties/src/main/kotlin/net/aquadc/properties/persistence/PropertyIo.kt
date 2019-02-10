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
import java.util.EnumSet

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

    @Deprecated("not sure whether it is useful", level = DeprecationLevel.ERROR)
    fun chars(prop: MutableProperty<CharArray>)

    @Deprecated("not sure whether it is useful", level = DeprecationLevel.ERROR)
    fun ints(prop: MutableProperty<IntArray>)

    @Deprecated("not sure whether it is useful", level = DeprecationLevel.ERROR)
    fun longs(prop: MutableProperty<LongArray>)

    @Deprecated("not sure whether it is useful", level = DeprecationLevel.ERROR)
    fun floats(prop: MutableProperty<FloatArray>)

    @Deprecated("not sure whether it is useful", level = DeprecationLevel.ERROR)
    fun doubles(prop: MutableProperty<DoubleArray>)

    @Deprecated("should create collection(string) instance", level = DeprecationLevel.ERROR)
    fun stringList(prop: MutableProperty<List<String>>)

    @Deprecated("should create custom DataType instance", level = DeprecationLevel.ERROR)
    fun <E : Enum<E>> enum(prop: MutableProperty<E>, type: Class<E>)

    @Deprecated("should create custom DataType instance", level = DeprecationLevel.ERROR)
    fun <E : Enum<E>> enumSet(prop: MutableProperty<Set<E>>, type: Class<E>)
}

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

@Deprecated("not sure whether it is useful", level = DeprecationLevel.ERROR)
@JvmName("chars") inline infix fun PropertyIo.x(prop: MutableProperty<CharArray>): Unit = throw UnsupportedOperationException()

@Deprecated("not sure whether it is useful", level = DeprecationLevel.ERROR)
@JvmName("ints") inline infix fun PropertyIo.x(prop: MutableProperty<IntArray>): Unit = throw UnsupportedOperationException()

@Deprecated("not sure whether it is useful", level = DeprecationLevel.ERROR)
@JvmName("longs") inline infix fun PropertyIo.x(prop: MutableProperty<LongArray>): Unit = throw UnsupportedOperationException()

@Deprecated("not sure whether it is useful", level = DeprecationLevel.ERROR)
@JvmName("floats") inline infix fun PropertyIo.x(prop: MutableProperty<FloatArray>): Unit = throw UnsupportedOperationException()

@Deprecated("not sure whether it is useful", level = DeprecationLevel.ERROR)
@JvmName("doubles") inline infix fun PropertyIo.x(prop: MutableProperty<DoubleArray>): Unit = throw UnsupportedOperationException()

/** Reads or writes a [String] */
@JvmName("string") inline infix fun PropertyIo.x(prop: MutableProperty<String>): Unit =
        string.invoke(prop)

private val StringList = collection(string)
/** Reads or writes a [List] of [String]s */
@JvmName("stringList") infix fun PropertyIo.x(prop: MutableProperty<List<String>>): Unit = StringList(prop)

@Deprecated("internally this uses reflection. Should use own DataType instead", level = DeprecationLevel.ERROR)
@JvmName("enum") inline infix fun <reified E : Enum<E>> PropertyIo.x(prop: MutableProperty<E>): Unit = throw UnsupportedOperationException()

@Deprecated("Should use own DataType instead", level = DeprecationLevel.ERROR)
@JvmName("enumSet") inline infix fun <reified E : Enum<E>> PropertyIo.x(prop: MutableProperty<Set<E>>): Unit = throw UnsupportedOperationException()

@Deprecated("Should use own DataType instead", level = DeprecationLevel.ERROR)
@JvmName("realEnumSet") inline infix fun <reified E : Enum<E>> PropertyIo.x(prop: MutableProperty<EnumSet<E>>): Unit = throw UnsupportedOperationException()
