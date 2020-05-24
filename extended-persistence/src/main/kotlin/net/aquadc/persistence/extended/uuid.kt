@file:JvmName("Uuids")
package net.aquadc.persistence.extended

import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.SimpleValue
import java.util.UUID


val uuid: DataType.NotNull.Simple<UUID> = object : StringableSimpleType<UUID>(Kind.Blob) {
    override fun load(value: SimpleValue): UUID =
        if (value is CharSequence) UUID.fromString(value.toString())
        else fromBytes(value as ByteArray)
    override fun store(value: UUID): SimpleValue = toBytes(value.mostSignificantBits, value.leastSignificantBits)
    override fun storeAsString(value: UUID): CharSequence = value.toString()

    // copy-paste from https://stackoverflow.com/a/27610608/3050249
    private fun fromBytes(b: ByteArray): UUID = UUID(
        b2l(b[0], b[1], b[2], b[3], b[4], b[5], b[6], b[7]), b2l(b[8], b[9], b[10], b[11], b[12], b[13], b[14], b[15])
    )
    private fun b2l(b7: Byte, b6: Byte, b5: Byte, b4: Byte, b3: Byte, b2: Byte, b1: Byte, b0: Byte) =
        (b7.toLong() shl 56) or
            (b6.toLong() and 0xff shl 48) or
            (b5.toLong() and 0xff shl 40) or
            (b4.toLong() and 0xff shl 32) or
            (b3.toLong() and 0xff shl 24) or
            (b2.toLong() and 0xff shl 16) or
            (b1.toLong() and 0xff shl 8) or
            (b0.toLong() and 0xff)
    private fun toBytes(hi: Long, lo: Long): ByteArray = byteArrayOf(
        (hi shr 56).toByte(),
        (hi shr 48).toByte(),
        (hi shr 40).toByte(),
        (hi shr 32).toByte(),
        (hi shr 24).toByte(),
        (hi shr 16).toByte(),
        (hi shr 8).toByte(),
        hi.toByte(),
        (lo shr 56).toByte(),
        (lo shr 48).toByte(),
        (lo shr 40).toByte(),
        (lo shr 32).toByte(),
        (lo shr 24).toByte(),
        (lo shr 16).toByte(),
        (lo shr 8).toByte(),
        lo.toByte()
    )
}
