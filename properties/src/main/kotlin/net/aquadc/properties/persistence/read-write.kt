package net.aquadc.properties.persistence

import net.aquadc.persistence.stream.CleverDataInput
import net.aquadc.persistence.stream.CleverDataOutput
import net.aquadc.persistence.type.DataType

internal fun <T> DataType<T>.write(output: CleverDataOutput, value: T) {
    check(value !== null || isNullable)

    // these values can simply be put into stream
    when (this) {
        is DataType.String -> return output.writeString(value?.let(::asString))
        is DataType.Blob -> return output.writeBytes(value?.let(::asByteArray))
        is DataType.Integer -> {
            if (sizeBits == 1) {
                return output.writeByte(value?.let { if (asNumber(it) as Boolean) 1 else 0 } ?: -1)
            }
        }
    }

    // these values cannot preserve nullability info, write it ourselves
    if (isNullable) {
        val isNull = value == null
        output.writeByte(if (isNull) -1 else 0)
        if (isNull) return
    }

    return when (this) {
        is DataType.Integer -> {
            val num = asNumber(value)
            when (sizeBits) {
                // 1 was already handled
                8 -> output.writeByte((num as Byte).toInt())
                16 -> output.writeShort((num as Short).toInt())
                32 -> output.writeInt(num as Int)
                64 -> output.writeLong(num as Long)
                else -> throw AssertionError()
            }
        }
        is DataType.Float -> {
            val num = asNumber(value)
            when (sizeBits) {
                32 -> output.writeInt(java.lang.Float.floatToIntBits(num as Float))
                64 -> output.writeLong(java.lang.Double.doubleToLongBits(value as Double))
                else -> throw AssertionError()
            }
        }
        is DataType.String,
        is DataType.Blob -> throw AssertionError()
    }
}

@Suppress("UNCHECKED_CAST")
internal fun <T> DataType<T>.read(input: CleverDataInput): T {
    when (this) {
        is DataType.String -> {
            val str = input.readString()
            return if (str == null) {
                check(isNullable); null
            } else {
                asT(str)
            } as T
        }
        is DataType.Blob -> {
            val bytes = input.readBytes()
            return if (bytes == null) {
                check(isNullable); null
            } else {
                asT(bytes)
            } as T
        }
        is DataType.Integer -> {
            if (sizeBits == 1) {
                val bool = input.readByte().toInt()
                return if (bool == -1) {
                    check(isNullable); null
                } else {
                    asT(bool == 1)
                } as T
            }
        }
    }

    if (isNullable && input.readByte() == (-1).toByte()) {
        return null as T
    }

    return when (this) {
        is DataType.Integer -> asT(when (sizeBits) {
            // 1 was already handled
            8 -> input.readByte()
            16 -> input.readShort()
            32 -> input.readInt()
            64 -> input.readLong()
            else -> throw AssertionError()
        })
        is DataType.Float -> asT(when (sizeBits) {
            32 -> java.lang.Float.intBitsToFloat(input.readInt())
            64 -> java.lang.Double.longBitsToDouble(input.readLong())
            else -> throw AssertionError()
        })

        is DataType.String, is DataType.Blob -> throw AssertionError()
    }
}
