package net.aquadc.properties.android.persistence

import android.os.Parcel
import net.aquadc.persistence.stream.BetterDataInput
import net.aquadc.persistence.stream.BetterDataOutput

/**
 * Adapts several [Parcel] methods to conform [BetterDataInput] and [BetterDataOutput].
 */
object ParcelIo : BetterDataInput<Parcel>, BetterDataOutput<Parcel> {

    // input

    override fun readByte(input: Parcel): Byte =
            input.readByte()

    override fun readShort(input: Parcel): Short =
            input.readInt().assertFitsShort()

    override fun readInt(input: Parcel): Int =
            input.readInt()

    override fun readLong(input: Parcel): Long =
            input.readLong()

    override fun readBytes(input: Parcel): ByteArray? =
            input.createByteArray()

    override fun readString(input: Parcel): String? =
            input.readString()

    // output

    override fun writeByte(output: Parcel, byte: Byte) =
            output.writeByte(byte)

    override fun writeShort(output: Parcel, short: Short) =
            output.writeInt(short.toInt())

    override fun writeInt(output: Parcel, int: Int) =
            output.writeInt(int)

    override fun writeLong(output: Parcel, long: Long) =
            output.writeLong(long)

    override fun writeBytes(output: Parcel, bytes: ByteArray?) =
            output.writeByteArray(bytes)

    override fun writeString(output: Parcel, string: String?) =
            output.writeString(string)

}
