package net.aquadc.persistence.stream

import java.io.DataInput
import java.io.DataOutput


abstract class UnusedDataInputMethods : DataInput {

    final override fun readFully(b: ByteArray?): Unit =
            throw UnsupportedOperationException()

    final override fun readFully(b: ByteArray?, off: Int, len: Int): Unit =
            throw UnsupportedOperationException()

    final override fun skipBytes(n: Int): Int =
            throw UnsupportedOperationException()

    final override fun readBoolean(): Boolean =
            throw UnsupportedOperationException()

    final override fun readUnsignedByte(): Int =
            throw UnsupportedOperationException()

    final override fun readUnsignedShort(): Int =
            throw UnsupportedOperationException()

    final override fun readChar(): Char =
            throw UnsupportedOperationException()

    final override fun readFloat(): Float =
            throw UnsupportedOperationException()

    final override fun readDouble(): Double =
            throw UnsupportedOperationException()

    final override fun readLine(): String =
            throw UnsupportedOperationException()

    final override fun readUTF(): String? =
            throw UnsupportedOperationException()

}

abstract class UnusedDataOutputMethods : DataOutput {

    final override fun write(b: Int): Unit =
            throw UnsupportedOperationException()

    final override fun write(b: ByteArray?): Unit =
            throw UnsupportedOperationException()

    final override fun write(b: ByteArray?, off: Int, len: Int): Unit =
            throw UnsupportedOperationException()

    final override fun writeBoolean(v: Boolean): Unit =
            throw UnsupportedOperationException()

    final override fun writeChar(v: Int): Unit =
            throw UnsupportedOperationException()

    final override fun writeFloat(v: Float): Unit =
            throw UnsupportedOperationException()

    final override fun writeDouble(v: Double): Unit =
            throw UnsupportedOperationException()

    final override fun writeBytes(s: String?): Unit =
            throw UnsupportedOperationException()

    final override fun writeChars(s: String?): Unit =
            throw UnsupportedOperationException()

    final override fun writeUTF(s: String?): Unit =
            throw UnsupportedOperationException()

}
