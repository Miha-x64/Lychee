package net.aquadc.persistence.type


/**
 * Represents a way of storing a value.
 * 'Encoded' type is Any to avoid creating many different types inside a sealed class —
 * that would impede decorating existing [DataType] implementations.
 */
sealed class DataType<T> {

    /**
     * Converts persistable value into its in-memory representation.
     * @return in-memory view on [value]
     */
    abstract fun decode(value: Any?): T

    /**
     * Converts in-memory value into its persistable representation.
     * @return persistable view on [value]
     */
    abstract fun encode(value: T): Any?


    /**
     * Adds nullability to runtime representation of [actualType].
     * Wraps only non-nullable type.
     * (However, some non-standard [Simple] or [Collect] type instances can easily be nullable themselves —
     * this means [decode] may return and [encode] may accept `null`s, but not vice versa.)
     */
    class Nullable<T : Any>(
            val actualType: DataType<T>
    ) : DataType<T?>() {

        override fun decode(value: Any?): T? =
                if (value === null) null else actualType.decode(value)

        override fun encode(value: T?): Any? =
                if (value === null) null else actualType.encode(value)

    }

    /**
     * A simple, non-composite (and thus easily composable) type.
     * [decode] must accept and [encode] must return objects of type which strictly depends on [kind].
     */
    abstract class Simple<T>(
            val kind: Kind
    ) : DataType<T>() {

        enum class Kind {
            Bool,
            I8, I16, I32, I64,
            F32, F64,
            Str, Blob
        }

    }

    // TODO: move Schema here

    // TODO: Patch/Diff/Delta/Partial

    /**
     * A collection of elements of [elementType].
     * [C] is typically a collection, but it is not required.
     * May have [List] or [Set] semantics, depending on implementations
     * of both this data type and the underlying storage.
     */
    abstract class Collect<C, E>(
            val elementType: DataType<E>
    ) : DataType<C>() {

        final override fun decode(value: Any?): C =
                decode(value as Collection<Any?>)

        /**
         * Converts persistable collection value into its in-memory representation.
         * Elements are encoded by [elementType], so typical [decode] may look like
         * `SomeCollection(value.map(elementType::decode))`
         */
        abstract fun decode(value: Collection<Any?>): C

        // inheritDoc
        /**
         * Elements must be encoed by [elementType], so typical [encode] may look like
         * `value.map(elementType::encode)`
         */
        abstract override fun encode(value: C): Collection<Any?>

    }


//    abstract class Dictionary<M, K, V> internal constructor(isNullable: Boolean, keyType: DataType<K>, valueType: DataType<K>) : DataType<M>(isNullable) TODO
//    abstract class Union<T> internal constructor(isNullable: Boolean, types: List<DataType<out T>>) : DataType<T>(isNullable) TODO
//    abstract class Struct<T> internal constructor(isNullable: Boolean, def: StructDef<T>) : DataType<T>(isNullable) TODO
//    Date is not supported because it's mutable and the most parts of it are deprecated 20+ years ago. TODO consider


}

/**
 * Match/visit (on) this [dataType], passing [arg] and [payload] into [this] visitor.
 */
@Suppress("NOTHING_TO_INLINE") // hope this will generate monomorphic call-site
inline fun <PL, ARG, T, R> DataTypeVisitor<PL, ARG, T, R>.match(dataType: DataType<T>, payload: PL, arg: ARG): R =
    when (dataType) {
        is DataType.Nullable<*> -> {
            @Suppress("UNCHECKED_CAST")
            val actualType = dataType.actualType as DataType<T> // T is narrowed to T!!

            when (actualType) {
                is DataType.Nullable<*> -> throw AssertionError()
                is DataType.Simple -> payload.simple(arg, dataType, actualType.kind)
                is DataType.Collect<*, *> -> payload.collection(arg, dataType, actualType as DataType.Collect<T, *>)
            }
        }
        is DataType.Simple -> payload.simple(arg, dataType, dataType.kind)
        is DataType.Collect<T, *> -> payload.collection(arg, dataType, dataType)
    }

interface DataTypeVisitor<PL, ARG, T, R> {
    fun PL.simple(arg: ARG, raw: DataType<T>, kind: DataType.Simple.Kind): R
    fun <E> PL.collection(arg: ARG, raw: DataType<T>, type: DataType.Collect<T, E>): R
}
