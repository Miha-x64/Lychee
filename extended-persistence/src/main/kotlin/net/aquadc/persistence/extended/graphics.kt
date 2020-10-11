@file:JvmName("Graphics")
package net.aquadc.persistence.extended

import net.aquadc.persistence.HEX_ARRAY
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.SimpleValue

/**
 * Represents AARRGGBB colour as an [Int].
 * For text formats, uses #AARRGGBB; when alpha==FF, uses #RRGGBB.
 */
@JvmField val colour: DataType.NotNull.Simple<Int> = object : StringableSimpleType<Int>(Kind.I32) {
    override fun load(value: SimpleValue): Int =
        if (value is CharSequence) parse(value) else value as Int
    override fun store(value: Int): SimpleValue = value

    private fun parse(value: CharSequence): Int {
        check(value[0] == '#') { value }
        return when (value.length) {
            7 -> (0xFF000000L or java.lang.Long.parseLong(value.subSequence(1, 7).toString(), 16))
            9 -> java.lang.Long.parseLong(value.subSequence(1, 9).toString(), 16)
            else -> error(value)
        }.toInt()
    }
    // it would be more OOPish to implement CharSequence wrapping Int,
    // but we know it will end up with toString() 😿
    override fun storeAsString(value: Int): CharSequence = String(
        if ((value.toLong() and 0xFF000000L) == 0xFF000000L) byteArrayOf('#'.toByte(),
            // don't write FF for alpha
            HEX_ARRAY[value ushr 20 and 15], HEX_ARRAY[value ushr 16 and 15],
            HEX_ARRAY[value ushr 12 and 15], HEX_ARRAY[value ushr 8 and 15],
            HEX_ARRAY[value ushr 4 and 15], HEX_ARRAY[value and 15]
        ) else byteArrayOf('#'.toByte(),
            HEX_ARRAY[value ushr 28 and 15], HEX_ARRAY[value ushr 24 and 15],
            HEX_ARRAY[value ushr 20 and 15], HEX_ARRAY[value ushr 16 and 15],
            HEX_ARRAY[value ushr 12 and 15], HEX_ARRAY[value ushr 8 and 15],
            HEX_ARRAY[value ushr 4 and 15], HEX_ARRAY[value and 15]
        )
    )

}
