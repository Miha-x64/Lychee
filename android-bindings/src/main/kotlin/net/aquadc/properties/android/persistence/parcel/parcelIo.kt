package net.aquadc.properties.android.persistence.parcel

import android.os.Parcel
import net.aquadc.persistence.stream.BetterDataInput
import net.aquadc.persistence.stream.BetterDataOutput
import net.aquadc.persistence.stream.StreamReaderVisitor
import net.aquadc.persistence.stream.StreamWriterVisitor
import net.aquadc.persistence.type.DataTypeVisitor
import net.aquadc.properties.android.persistence.assertFitsShort

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

    private val readVis = StreamReaderVisitor<Parcel, Any?>(this)
    override fun <TYPE> readVisitor(): DataTypeVisitor<Parcel, Nothing?, TYPE, Any?> =
            readVis as DataTypeVisitor<Parcel, Nothing?, TYPE, Any?>

    // output

    override fun writeByte(output: Parcel, byte: Byte): Unit =
            output.writeByte(byte)

    override fun writeShort(output: Parcel, short: Short): Unit =
            output.writeInt(short.toInt())

    override fun writeInt(output: Parcel, int: Int): Unit =
            output.writeInt(int)

    override fun writeLong(output: Parcel, long: Long): Unit =
            output.writeLong(long)

    override fun writeBytes(output: Parcel, bytes: ByteArray?): Unit =
            output.writeByteArray(bytes)

    override fun writeString(output: Parcel, string: String?): Unit =
            output.writeString(string)

    private val writer = StreamWriterVisitor<Parcel, Any?>(this)
    override fun <TYPE> writerVisitor(): DataTypeVisitor<Parcel, Any?, TYPE, Unit> =
            writer as DataTypeVisitor<Parcel, Any?, TYPE, Unit>

}
