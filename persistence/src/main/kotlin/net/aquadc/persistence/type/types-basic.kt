@file:JvmName("BasicTypes")
package net.aquadc.persistence.type


private class SimpleNoOp<T>(kind: Kind) : DataType.Simple<T>(kind) {

    /**
     * {@implNote does nothing but sanity checks}
     */
    override fun encode(value: T): Any? {
        sanityCheck(value)
        return value
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

@JvmField val string: DataType.Simple<String> = SimpleNoOp(DataType.Simple.Kind.Str)


private const val bytesMessage =
        "Note: if you mutate array, we won't notice — you must set() it in a transaction. " +
                "Consider using immutable ByteString instead."

@Deprecated(bytesMessage, ReplaceWith("byteString"))
@JvmField val byteArray: DataType.Simple<ByteArray> = SimpleNoOp(DataType.Simple.Kind.Blob)

@JvmField val bool: DataType.Simple<Boolean> = SimpleNoOp(DataType.Simple.Kind.Bool)

@JvmField val byte: DataType.Simple<Byte> = SimpleNoOp(DataType.Simple.Kind.I8)

@JvmField val short: DataType.Simple<Short> = SimpleNoOp(DataType.Simple.Kind.I16)

@JvmField val int: DataType.Simple<Int> = SimpleNoOp(DataType.Simple.Kind.I32)

@JvmField val long: DataType.Simple<Long> = SimpleNoOp(DataType.Simple.Kind.I64)


@JvmField val float: DataType.Simple<Float> = SimpleNoOp(DataType.Simple.Kind.F32)

@JvmField val double: DataType.Simple<Double> = SimpleNoOp(DataType.Simple.Kind.F64)


@Suppress("NOTHING_TO_INLINE")
inline fun <T : Any> nullable(type: DataType<T>): DataType.Nullable<T> =
        DataType.Nullable(type)

private abstract class CollectBase<C : Collection<E>, E : Any?>(elementType: DataType<E>) : DataType.Collect<C, E>(elementType) {

    /**
     * {@implNote does nothing but sanity checks}
     */
    override fun encode(value: C): Collection<Any?> =
            value.map(elementType::encode)

}

fun <E> collection(elementType: DataType<E>): DataType.Collect<Collection<E>, E> =
        object : CollectBase<Collection<E>, E>(elementType) {
            override fun decode(value: Collection<Any?>): Collection<E> =
                    value.map(this.elementType::decode)
        }

fun <E> set(elementType: DataType<E>): DataType.Collect<Set<E>, E> =
        object : CollectBase<Set<E>, E>(elementType) {
            override fun decode(value: Collection<Any?>): Set<E> =
                    value.mapTo(HashSet(), this.elementType::decode) // todo ArraySet
        }

@Deprecated(
        message = "List semantics cannot be guaranteed: duplicates handling depends on the underlying storage",
        level = DeprecationLevel.ERROR
)
fun <E> list(elementType: DataType<E>): Nothing =
        throw UnsupportedOperationException()
