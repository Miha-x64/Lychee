package net.aquadc.persistence.stream

import java.io.*

/**
 * More usable [DataOutput] subtype.
 * Methods from [UnusedDataOutputMethods] won't be used.
 * @see BetterDataOutputStream
 */
interface BetterDataOutput : DataOutput {

    /**
     * Writes the given [bytes] including its nullability info, and length, if applicable.
     */
    fun writeBytes(bytes: ByteArray?)

    /**
     * Writes the given [string] including its nullability information.
     */
    fun writeString(string: String?)

}

/**
 * More usable [DataInput] subtype.
 * Methods from [UnusedDataInputMethods] won't be used.
 * @see BetterDataInputStream
 */
interface BetterDataInput : DataInput {

    /**
     * Reads a [ByteArray] from this stream.
     */
    fun readBytes(): ByteArray?

    /**
     * Reads a [String] including its nullability information.
     */
    fun readString(): String?

}

/**
 * Simple [BetterDataOutput] implementation — a more usable data output stream.
 */
class BetterDataOutputStream(output: OutputStream) : DataOutputStream(output), BetterDataOutput {

    override fun writeBytes(bytes: ByteArray?) {
        if (bytes == null) {
            writeInt(-1)
        } else {
            writeInt(bytes.size)
            write(bytes)
        }
    }

    override fun writeString(string: String?) {
        if (string == null) {
            write(-1)
        } else {
            write(1)
            writeUTF(string)
        }
    }

}

/**
 * Simple [BetterDataInput] implementation — a more usable data input stream.
 */
class BetterDataInputStream(input: InputStream) : DataInputStream(input), BetterDataInput {

    override fun readBytes(): ByteArray? {
        val size = readInt()
        return if (size == -1) null else ByteArray(size).also { readFully(it, 0, size) }
    }

    override fun readString(): String? = when (readByte()) {
        (-1).toByte() -> null
        1.toByte() -> readUTF()
        else -> throw AssertionError()
    }

}
