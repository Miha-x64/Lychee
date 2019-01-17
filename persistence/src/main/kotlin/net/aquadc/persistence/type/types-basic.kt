@file:JvmName("BasicTypes")
package net.aquadc.persistence.type


private class NoOp<T>(kind: Kind) : DataType.Simple<T>(kind) {

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
    override fun decode(value: Any?): T {
        sanityCheck(value)
        return value as T
    }

    private fun sanityCheck(value: Any?) {
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

@JvmField val string: DataType.Simple<String> = NoOp(DataType.Simple.Kind.Str)


private const val bytesMessage =
        "Note: if you mutate array, we won't notice — you must set() it in a transaction. " +
                "Consider using immutable ByteString instead."

@Deprecated(bytesMessage, ReplaceWith("byteString"))
@JvmField val byteArray: DataType.Simple<ByteArray> = NoOp(DataType.Simple.Kind.Blob)

@JvmField val bool: DataType.Simple<Boolean> = NoOp(DataType.Simple.Kind.Bool)

@JvmField val byte: DataType.Simple<Byte> = NoOp(DataType.Simple.Kind.I8)

@JvmField val short: DataType.Simple<Short> = NoOp(DataType.Simple.Kind.I16)

@JvmField val int: DataType.Simple<Int> = NoOp(DataType.Simple.Kind.I32)

@JvmField val long: DataType.Simple<Long> = NoOp(DataType.Simple.Kind.I64)


@JvmField val float: DataType.Simple<Float> = NoOp(DataType.Simple.Kind.F32)

@JvmField val double: DataType.Simple<Double> = NoOp(DataType.Simple.Kind.F64)


inline fun <T : Any> nullable(type: DataType<T>): DataType.Nullable<T> =
        DataType.Nullable(type)
