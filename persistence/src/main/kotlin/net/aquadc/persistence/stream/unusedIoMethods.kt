package net.aquadc.persistence.stream

import java.io.DataInput
import java.io.DataOutput


abstract class UnusedDataInputMethods : DataInput {

    override fun readFully(b: ByteArray?): Unit =
            throw UnsupportedOperationException()

    override fun readFully(b: ByteArray?, off: Int, len: Int): Unit =
            throw UnsupportedOperationException()

    override fun skipBytes(n: Int): Int =
            throw java.lang.UnsupportedOperationException()

    override fun readBoolean(): Boolean =
            throw UnsupportedOperationException()

    override fun readUnsignedByte(): Int =
            throw UnsupportedOperationException()

    override fun readUnsignedShort(): Int =
            throw UnsupportedOperationException()

    override fun readChar(): Char =
            throw UnsupportedOperationException()

    override fun readFloat(): Float =
            throw UnsupportedOperationException()

    override fun readDouble(): Double =
            throw UnsupportedOperationException()

    override fun readLine(): String =
            throw UnsupportedOperationException()

    override fun readUTF(): String? =
            throw UnsupportedOperationException()

}

abstract class UnusedDataOutputMethods : DataOutput {

    override fun write(b: Int): Unit =
            throw UnsupportedOperationException()

    override fun write(b: ByteArray?): Unit =
            throw UnsupportedOperationException()

    override fun write(b: ByteArray?, off: Int, len: Int): Unit =
            throw UnsupportedOperationException()

    override fun writeBoolean(v: Boolean): Unit =
            throw UnsupportedOperationException()

    override fun writeChar(v: Int): Unit =
            throw UnsupportedOperationException()

    override fun writeFloat(v: Float): Unit =
            throw UnsupportedOperationException()

    override fun writeDouble(v: Double): Unit =
            throw UnsupportedOperationException()

    override fun writeBytes(s: String?): Unit =
            throw UnsupportedOperationException()

    override fun writeChars(s: String?): Unit =
            throw UnsupportedOperationException()

    override fun writeUTF(s: String?): Unit =
            throw UnsupportedOperationException()

}
