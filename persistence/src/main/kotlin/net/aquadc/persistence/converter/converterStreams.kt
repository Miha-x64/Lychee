package net.aquadc.persistence.converter

import android.os.Parcel
import java.io.DataInput
import java.io.DataOutput


/**
 * A converter for Java's [DataInput] and [DataOutput].
 */
interface DataIoConverter<T> : Converter<T> {

    /**
     * Writes the [value] into [output].
     */
    fun write(output: DataOutput, value: T)

    /**
     * Reads a value from [input].
     */
    fun read(input: DataInput): T

}

/**
 * A converter for Android's [Parcel].
 */
interface ParcelConverter<T> : Converter<T> {

    /**
     * Writes the [value] into [output].
     */
    fun write(destination: Parcel, value: T)

    /**
     * Reads a value from [input].
     */
    fun read(source: Parcel): T

}
