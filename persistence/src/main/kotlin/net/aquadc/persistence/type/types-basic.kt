@file:JvmName("BasicTypes")
package net.aquadc.persistence.type


private class NoOp<T>(isNullable: Boolean, kind: Kind) : DataType.Simple<T>(isNullable, kind) {

    /**
     * {@implNote does nothing but sanity checks}
     */
    override fun encode(value: T): Any {
        sanityCheck(value)
        return value!!
    }

    /**
     * {@implNote does nothing but sanity checks}
     */
    @Suppress("UNCHECKED_CAST")
    override fun decode(value: Any): T {
        sanityCheck(value)
        return value as T
    }

    private fun sanityCheck(value: Any?) { // TODO: add a ProGuard rule to remove it
        if (value === null) throw NullPointerException()
        when (kind) {
            DataType.Simple.Kind.Bool -> value as Boolean
            DataType.Simple.Kind.I8 -> value as Byte
            DataType.Simple.Kind.I16 -> value as Short
            DataType.Simple.Kind.I32 -> value as Int
            DataType.Simple.Kind.I64 -> value as Long
            DataType.Simple.Kind.F32 -> value as Float
            DataType.Simple.Kind.F64 -> value as Double
            DataType.Simple.Kind.Str -> value as String
            DataType.Simple.Kind.Blob -> value as ByteArray
        }
    }

}

/*
@JvmField val smallString: DataType.Simple<String> = NoOp(false, DataType.Simple.Kind.TinyStr)
@JvmField val smallNullableString: DataType.Simple<String?> = NoOp(true, DataType.Simple.Kind.TinyStr)

@JvmField val mediumString: DataType.Simple<String> = NoOp(false, DataType.Simple.Kind.Str)
@JvmField val mediumNullableString: DataType.Simple<String?> = NoOp(true, DataType.Simple.Kind.Str)

@JvmField val largeString: DataType.Simple<String> = NoOp(false, DataType.Simple.Kind.BigStr)
@JvmField val largeNullableString: DataType.Simple<String?> = NoOp(true, DataType.Simple.Kind.BigStr)
*/
@JvmField val string: DataType.Simple<String> = NoOp(false, DataType.Simple.Kind.Str)
@JvmField val nullableString: DataType.Simple<String?> = NoOp(true, DataType.Simple.Kind.Str)


private const val bytesMessage =
        "Note: if you mutate array, we won't notice — you must set() it in a transaction. " +
                "Consider using immutable ByteString instead."

/*
@JvmField val smallByteArray: DataType<ByteArray> = Bytes(false, DataType.Simple.Kind.TinyBlob)
@JvmField val smallNullableByteArray: DataType<ByteArray?> = Bytes(true, DataType.Simple.Kind.TinyBlob)

@JvmField val mediumByteArray: DataType<ByteArray> = Bytes(false, DataType.Simple.Kind.Blob)
@JvmField val mediumNullableByteArray: DataType<ByteArray?> = Bytes(true, DataType.Simple.Kind.Blob)

@JvmField val largeByteArray: DataType<ByteArray> = Bytes(false, DataType.Simple.Kind.BigBlob)
@JvmField val largeNullableByteArray: DataType<ByteArray?> = Bytes(true, DataType.Simple.Kind.BigBlob)
*/
@Deprecated(bytesMessage, ReplaceWith("byteString"))
@JvmField val byteArray: DataType.Simple<ByteArray> = NoOp(false, DataType.Simple.Kind.Blob)

@Deprecated(bytesMessage, ReplaceWith("nullableByteString"))
@JvmField val nullableByteArray: DataType.Simple<ByteArray?> = NoOp(true, DataType.Simple.Kind.Blob)

@JvmField val bool: DataType.Simple<Boolean> = NoOp(false, DataType.Simple.Kind.Bool)
@JvmField val nullableBool: DataType.Simple<Boolean?> = NoOp(true, DataType.Simple.Kind.Bool)

@JvmField val byte: DataType.Simple<Byte> = NoOp(false, DataType.Simple.Kind.I8)
@JvmField val nullableByte: DataType.Simple<Byte?> = NoOp(true, DataType.Simple.Kind.I8)

@JvmField val short: DataType.Simple<Short> = NoOp(false, DataType.Simple.Kind.I16)
@JvmField val nullableShort: DataType.Simple<Short?> = NoOp(true, DataType.Simple.Kind.I16)

@JvmField val int: DataType.Simple<Int> = NoOp(false, DataType.Simple.Kind.I32)
@JvmField val nullableInt: DataType.Simple<Int?> = NoOp(true, DataType.Simple.Kind.I32)

@JvmField val long: DataType.Simple<Long> = NoOp(false, DataType.Simple.Kind.I64)
@JvmField val nullableLong: DataType.Simple<Long?> = NoOp(true, DataType.Simple.Kind.I64)


@JvmField val float: DataType.Simple<Float> = NoOp(false, DataType.Simple.Kind.F32)
@JvmField val nullableFloat: DataType.Simple<Float?> = NoOp(true, DataType.Simple.Kind.F32)

@JvmField val double: DataType.Simple<Double> = NoOp(false, DataType.Simple.Kind.F64)
@JvmField val nullableDouble: DataType.Simple<Double?> = NoOp(true, DataType.Simple.Kind.F64)
