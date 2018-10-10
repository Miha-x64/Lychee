package net.aquadc.persistence.type

import net.aquadc.persistence.stream.CleverDataInput
import net.aquadc.persistence.stream.CleverDataOutput
import java.io.DataInput
import java.io.DataOutput


/**
 * A converter for [CleverDataOutput] and [CleverDataInput]
 * which are enhanced versions of [DataOutput] and [DataInput]
 * and just wrappers around [java.io.DataOutputStream] and [java.io.DataInputStream].
 */
interface DataIoConverter<T> : Converter<T> {

    /**
     * Writes the [value] into [output].
     */
    fun write(output: CleverDataOutput, value: T)

    /**
     * Reads a value from [input].
     */
    fun read(input: CleverDataInput): T

}
