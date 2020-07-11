package net.aquadc.persistence.stream

import java.io.DataInput
import java.io.DataInputStream
import java.io.DataOutput
import java.io.DataOutputStream

/**
 * Proxy making [DataOutput] more usable.
 * Required to abstract away both [DataOutputStream] and Android's Parcel.
 * @see DataStreams
 */
interface BetterDataOutput<D> {

    /**
     * Writes a single [byte] into [output].
     */
    fun writeByte(output: D, byte: Byte)

    /**
     * Writes a single [int] into [output].
     */
    fun writeInt(output: D, int: Int)

    /**
     * Writes a single [long] into [output].
     */
    fun writeLong(output: D, long: Long)

    /**
     * Writes the given [bytes] into [output], including its nullability info, and length, if applicable.
     */
    fun writeBytes(output: D, bytes: ByteArray?)

    /**
     * Writes the given [string] into [output], including its nullability information.
     */
    fun writeString(output: D, string: String?)

}

/**
 * Proxy making [DataInput] more usable.
 * Required to abstract away both [DataInputStream] and Android's Parcel.
 * @see DataStreams
 */
interface BetterDataInput<D> {

    /**
     * Reads a single byte from the [input].
     */
    fun readByte(input: D): Byte

    /**
     * Reads a single int from the [input].
     */
    fun readInt(input: D): Int

    /**
     * Reads a single long from the [input].
     */
    fun readLong(input: D): Long

    /**
     * Reads a [ByteArray] from the [input].
     */
    fun readBytes(input: D): ByteArray?

    /**
     * Reads a [String] from [input] including its nullability information.
     */
    fun readString(input: D): String?

}

/**
 * More usable [DataInput] and [DataOutput] streams.
 */
object DataStreams : BetterDataInput<DataInput>, BetterDataOutput<DataOutput> {

    // input

    override fun readByte(input: DataInput): Byte =
            input.readByte()

    override fun readInt(input: DataInput): Int =
            input.readInt()

    override fun readLong(input: DataInput): Long =
            input.readLong()

    override fun readBytes(input: DataInput): ByteArray? {
        val size = input.readInt()
        return if (size == -1) null else ByteArray(size).also { input.readFully(it, 0, size) }
    }

    override fun readString(input: DataInput): String? = when (input.readByte()) {
        (-1).toByte() -> null
        1.toByte() -> input.readUTF()
        else -> throw AssertionError()
    }

    // output

    override fun writeByte(output: DataOutput, byte: Byte): Unit =
            output.writeByte(byte.toInt())

    override fun writeInt(output: DataOutput, int: Int): Unit =
            output.writeInt(int)

    override fun writeLong(output: DataOutput, long: Long): Unit =
            output.writeLong(long)

    override fun writeBytes(output: DataOutput, bytes: ByteArray?) {
        if (bytes == null) {
            output.writeInt(-1)
        } else {
            output.writeInt(bytes.size)
            output.write(bytes)
        }
    }

    override fun writeString(output: DataOutput, string: String?) {
        if (string == null) {
            output.write(-1)
        } else {
            output.write(1)
            output.writeUTF(string)
        }
    }

}
