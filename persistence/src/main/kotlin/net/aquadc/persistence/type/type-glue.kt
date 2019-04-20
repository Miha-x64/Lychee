package net.aquadc.persistence.type

import net.aquadc.persistence.stream.DataStreams
import net.aquadc.persistence.stream.read
import net.aquadc.persistence.stream.write
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream


/**
 * Represents [T] as a [ByteArray] (blob).
 */
fun <T> serialized(type: DataType<T>): DataType.Simple<T> = object : DataType.Simple<T>(Kind.Blob) {

    override fun load(value: SimpleValue): T =
            type.read(DataStreams, DataInputStream(ByteArrayInputStream(value as ByteArray)))

    override fun store(value: T): SimpleValue =
            ByteArrayOutputStream().also {
                type.write(DataStreams, DataOutputStream(it), value)
            }.toByteArray()

}
