package net.aquadc.persistence.stream

import net.aquadc.persistence.type.DataTypeVisitor
import java.io.DataInput
import java.io.DataInputStream
import java.io.DataOutput
import java.io.DataOutputStream

/**
 * Proxy making [DataOutput] more usable.
 * Required to abstract away both [DataOutputStream] and Android's Parcel.
 * @see DataStreams
 */
interface BetterDataOutput<T> {

    /**
     * Writes a single [byte] into [output].
     */
    fun writeByte(output: T, byte: Byte)

    /**
     * Writes a single [short] into [output].
     */
    fun writeShort(output: T, short: Short)

    /**
     * Writes a single [int] into [output].
     */
    fun writeInt(output: T, int: Int)

    /**
     * Writes a single [long] into [output].
     */
    fun writeLong(output: T, long: Long)

    /**
     * Writes the given [bytes] into [output], including its nullability info, and length, if applicable.
     */
    fun writeBytes(output: T, bytes: ByteArray?)

    /**
     * Writes the given [string] into [output], including its nullability information.
     */
    fun writeString(output: T, string: String?)

    /**
     * Gives a visitor capable of writing already encoded values of type [TYPE] into [T].
     */
    fun <TYPE> writerVisitor(): DataTypeVisitor<T, Any?, TYPE, Unit>

}

/**
 * Proxy making [DataInput] more usable.
 * Required to abstract away both [DataInputStream] and Android's Parcel.
 * @see DataStreams
 */
interface BetterDataInput<T> {

    /**
     * Reads a single byte from the [input].
     */
    fun readByte(input: T): Byte

    /**
     * Reads a single short from the [input].
     */
    fun readShort(input: T): Short

    /**
     * Reads a single int from the [input].
     */
    fun readInt(input: T): Int

    /**
     * Reads a single long from the [input].
     */
    fun readLong(input: T): Long

    /**
     * Reads a [ByteArray] from the [input].
     */
    fun readBytes(input: T): ByteArray?

    /**
     * Reads a [String] from [input] including its nullability information.
     */
    fun readString(input: T): String?

    /**
     * Gives a visitor capable of reading raw/encoded value of type [TYPE] from [T].
     */
    fun <TYPE> readVisitor(): DataTypeVisitor<T, Nothing?, TYPE, Any?>

}

/**
 * More usable [DataInput] and [DataOutput] streams.
 */
object DataStreams : BetterDataInput<DataInput>, BetterDataOutput<DataOutput> {

    // input

    override fun readByte(input: DataInput): Byte =
            input.readByte()

    override fun readShort(input: DataInput): Short =
            input.readShort()

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

    private val reader = StreamReaderVisitor<DataInput, Any?>(this)
    override fun <TYPE> readVisitor(): DataTypeVisitor<DataInput, Nothing?, TYPE, Any?> =
            reader as DataTypeVisitor<DataInput, Nothing?, TYPE, Any?>

    // output

    override fun writeByte(output: DataOutput, byte: Byte): Unit =
            output.writeByte(byte.toInt())

    override fun writeShort(output: DataOutput, short: Short): Unit =
            output.writeShort(short.toInt())

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

    private val writer = StreamWriterVisitor<DataOutput, Any?>(this)
    override fun <TYPE> writerVisitor(): DataTypeVisitor<DataOutput, Any?, TYPE, Unit> =
            writer as DataTypeVisitor<DataOutput, Any?, TYPE, Unit>

}
