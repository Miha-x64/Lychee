package net.aquadc.persistence.stream

import net.aquadc.persistence.type.DataType


fun <T> DataType<T>.write(output: BetterDataOutput, value: T) {
    check(value !== null || isNullable)

    // these values can be put into stream along with nullability info
    when (this) {
        is DataType.Simple<T> -> {
            when (kind) {
                DataType.Simple.Kind.Bool ->
                    return output.writeByte(value?.let { if (encode(it) as Boolean) 1 else 0 } ?: -1)
                DataType.Simple.Kind.Str -> return output.writeString(value?.let { encode(it) as String })
                DataType.Simple.Kind.Blob -> return output.writeBytes(value?.let { encode(it) as ByteArray })
                else -> { /* continue */ }
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
        is DataType.Simple<T> -> {
            val num = encode(value)
            when (kind) {
                DataType.Simple.Kind.I8 -> output.writeByte((num as Byte).toInt())
                DataType.Simple.Kind.I16 -> output.writeShort((num as Short).toInt())
                DataType.Simple.Kind.I32 -> output.writeInt(num as Int)
                DataType.Simple.Kind.I64 -> output.writeLong(num as Long)
                DataType.Simple.Kind.F32 -> output.writeInt(java.lang.Float.floatToIntBits(num as Float))
                DataType.Simple.Kind.F64 -> output.writeLong(java.lang.Double.doubleToLongBits(value as Double))
                DataType.Simple.Kind.Bool, DataType.Simple.Kind.Str, DataType.Simple.Kind.Blob -> throw AssertionError()
            }
        }
    }
}

private val boolDictionary = arrayOf(null, false, true)
@Suppress("UNCHECKED_CAST")
fun <T> DataType<T>.read(input: BetterDataInput): T {
    when (this) {
        is DataType.Simple<T> -> {
            val value = when (kind) {
                DataType.Simple.Kind.Bool -> boolDictionary[input.readByte().toInt() + 1]
                DataType.Simple.Kind.Str -> input.readString()
                DataType.Simple.Kind.Blob -> input.readBytes()
                else -> boolDictionary // it's private — using as 'Unset' marker
            }

            if (value === null)
                return check(isNullable).let { null as T }

            if (value !== boolDictionary) // i. e. 'not unset'
                return decode(value)
        }
    }

    if (isNullable && input.readByte() == (-1).toByte()) {
        return null as T
    }

    return decode(when (this) {
        is DataType.Simple<T> -> {
            when (kind) {
                DataType.Simple.Kind.I8 -> input.readByte()
                DataType.Simple.Kind.I16 -> input.readShort()
                DataType.Simple.Kind.I32 -> input.readInt()
                DataType.Simple.Kind.I64 -> input.readLong()
                DataType.Simple.Kind.F32 -> java.lang.Float.intBitsToFloat(input.readInt())
                DataType.Simple.Kind.F64 -> java.lang.Double.longBitsToDouble(input.readLong())
                DataType.Simple.Kind.Bool, DataType.Simple.Kind.Str, DataType.Simple.Kind.Blob -> throw AssertionError()
            }
        }
    })
}
