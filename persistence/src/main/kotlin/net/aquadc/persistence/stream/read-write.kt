package net.aquadc.persistence.stream

import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.match


fun <D, T> DataType<T>.write(out: BetterDataOutput<D>, put: D, value: T): Unit = match { isNullable, simple ->
    // these values can be put into stream along with nullability info
    when (simple.kind) {
        DataType.Simple.Kind.Bool ->
            return out.writeByte(put,
                    value?.let { if (encode(it) as Boolean) 1.toByte() else 0.toByte() } ?: (-1).toByte()
            )
        DataType.Simple.Kind.Str -> return out.writeString(put, value?.let { encode(it) as String })
        DataType.Simple.Kind.Blob -> return out.writeBytes(put, value?.let { encode(it) as ByteArray })
        else -> { /* continue */ }
    }

    // these values cannot preserve nullability info, write it ourselves
    if (isNullable) {
        val isNull = value == null
        out.writeByte(put, if (isNull) -1 else 0)
        if (isNull) return
    }

    val num = encode(value)
    when (simple.kind) {
        DataType.Simple.Kind.I8 -> out.writeByte(put, num as Byte)
        DataType.Simple.Kind.I16 -> out.writeShort(put, num as Short)
        DataType.Simple.Kind.I32 -> out.writeInt(put, num as Int)
        DataType.Simple.Kind.I64 -> out.writeLong(put, num as Long)
        DataType.Simple.Kind.F32 -> out.writeInt(put, java.lang.Float.floatToIntBits(num as Float))
        DataType.Simple.Kind.F64 -> out.writeLong(put, java.lang.Double.doubleToLongBits(value as Double))
        DataType.Simple.Kind.Bool, DataType.Simple.Kind.Str, DataType.Simple.Kind.Blob -> throw AssertionError()
    }
}

private val boolDictionary = arrayOf(null, false, true)
@Suppress("UNCHECKED_CAST")
fun <D, T> DataType<T>.read(inp: BetterDataInput<D>, ut: D): T = match { isNullable, simple ->
    val value = when (simple.kind) {
        DataType.Simple.Kind.Bool -> boolDictionary[inp.readByte(ut).toInt() + 1]
        DataType.Simple.Kind.Str -> inp.readString(ut)
        DataType.Simple.Kind.Blob -> inp.readBytes(ut)
        else -> boolDictionary // it's private — using as 'Unset' marker
    }

    if (value === null)
        return check(isNullable).let { null as T }

    if (value !== boolDictionary) // i. e. 'not unset'
        return decode(value)

    // decode value with separated nullability info

    if (isNullable && inp.readByte(ut) == (-1).toByte()) {
        return null as T
    }

    return decode(when (simple.kind) {
        DataType.Simple.Kind.I8 -> inp.readByte(ut)
        DataType.Simple.Kind.I16 -> inp.readShort(ut)
        DataType.Simple.Kind.I32 -> inp.readInt(ut)
        DataType.Simple.Kind.I64 -> inp.readLong(ut)
        DataType.Simple.Kind.F32 -> java.lang.Float.intBitsToFloat(inp.readInt(ut))
        DataType.Simple.Kind.F64 -> java.lang.Double.longBitsToDouble(inp.readLong(ut))
        DataType.Simple.Kind.Bool, DataType.Simple.Kind.Str, DataType.Simple.Kind.Blob -> throw AssertionError()
    })
}
