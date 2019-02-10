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

/**
 * Describes [Boolean] instances.
 */
@JvmField val bool: DataType.Simple<Boolean> = SimpleNoOp(DataType.Simple.Kind.Bool)

/**
 * Describes [Byte] instances.
 */
@JvmField val byte: DataType.Simple<Byte> = SimpleNoOp(DataType.Simple.Kind.I8)

/**
 * Describes [Short] instances.
 */
@JvmField val short: DataType.Simple<Short> = SimpleNoOp(DataType.Simple.Kind.I16)

/**
 * Describes [Int] instances.
 */
@JvmField val int: DataType.Simple<Int> = SimpleNoOp(DataType.Simple.Kind.I32)

/**
 * Describes [Long] instances.
 */
@JvmField val long: DataType.Simple<Long> = SimpleNoOp(DataType.Simple.Kind.I64)


/**
 * Describes [Float] instances.
 */
@JvmField val float: DataType.Simple<Float> = SimpleNoOp(DataType.Simple.Kind.F32)

/**
 * Describes [Double] instances.
 */
@JvmField val double: DataType.Simple<Double> = SimpleNoOp(DataType.Simple.Kind.F64)


/**
 * Describes [String] instances.
 */
@JvmField val string: DataType.Simple<String> = SimpleNoOp(DataType.Simple.Kind.Str)

/**
 * Describes [ByteArray] instances.
 * Despite this declaration is deprecated, it is not going to disappear,
 * and [Deprecated.level] will remain [DeprecationLevel.WARNING].
 */
@Deprecated(
        "Note: if you mutate array, we won't notice — you must set() it in a transaction. " +
                "Consider using immutable ByteString instead.",
        ReplaceWith("byteString")
)
@JvmField val byteArray: DataType.Simple<ByteArray> = SimpleNoOp(DataType.Simple.Kind.Blob)

/**
 * Describes `T?` instances.
 */
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

/**
 * Represents a [Collection] of [E].
 * Despite it is represented as a [List], duplicates handling depends on the underlying storage.
 */
fun <E> collection(elementType: DataType<E>): DataType.Collect<List<E>, E> =
        object : CollectBase<List<E>, E>(elementType) {
            override fun decode(value: Collection<Any?>): List<E> =
                    value.map(this.elementType::decode)
        }

/**
 * Represents a [Set] of [E].
 */
fun <E> set(elementType: DataType<E>): DataType.Collect<Set<E>, E> =
        object : CollectBase<Set<E>, E>(elementType) {
            override fun decode(value: Collection<Any?>): Set<E> =
                    value.mapTo(HashSet(), this.elementType::decode) // todo ArraySet
        }

/**
 * A hint which makes [collection] easier to find.
 */
@Deprecated(
        "List semantics cannot be guaranteed: duplicates handling depends on the underlying storage",
        ReplaceWith("collection(elementType", "net.aquadc.persistence.type.collection"),
        DeprecationLevel.ERROR
)
fun <E> list(@Suppress("UNUSED_PARAMETER") elementType: DataType<E>): Nothing =
        throw UnsupportedOperationException()
