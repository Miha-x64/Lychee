package net.aquadc.persistence.stream

import android.os.Parcel
import java.io.DataInput
import java.io.DataOutput

/**
 * Adapts several [Parcel] methods to conform Java's [DataInput].
 */
class ParcelInput(
        private val input: Parcel
) : UnusedDataInputMethods(), CleverDataInput {

    override fun readByte(): Byte =
            input.readByte()

    override fun readShort(): Short {
        val int = input.readInt()
        if (int < Short.MIN_VALUE || int > Short.MAX_VALUE)
            throw ArithmeticException("$int cannot be interpreted as a 16-byte integer (Short)")
        return int.toShort()
    }

    override fun readInt(): Int =
            input.readInt()

    override fun readLong(): Long =
            input.readLong()

    override fun readBytes(): ByteArray? =
            input.createByteArray()

    override fun readString(): String? =
            input.readString()

}

/**
 * Adapts several [Parcel] methods to conform Java's [DataOutput].
 */
class ParcelOutput(
        private val output: Parcel
) : UnusedDataOutputMethods(), CleverDataOutput {

    override fun writeByte(v: Int): Unit =
            output.writeByte(v.toByte())

    override fun writeShort(v: Int) {
        if (v < Short.MIN_VALUE || v > Short.MAX_VALUE)
            throw ArithmeticException("$v cannot be interpreted as a 16-byte integer (Short)")
        output.writeInt(v)
    }

    override fun writeInt(v: Int): Unit =
            output.writeInt(v)

    override fun writeLong(v: Long): Unit =
            output.writeLong(v)

    override fun writeBytes(bytes: ByteArray?): Unit =
            output.writeByteArray(bytes)

    override fun writeString(string: String?): Unit =
            output.writeString(string)

}
