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
     * Adds nullability to [actualType].
     * Wraps only non-nullable type.
     * (However, [Simple] can easily be nullable.)
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

    /*abstract class SetOf<C : Set<T>?, T>(
            isNullable: Boolean,
            private val elementType: DataType<T>
    ) : DataType<C>(isNullable)*/


//    abstract class Dictionary<M, K, V> internal constructor(isNullable: Boolean, keyType: DataType<K>, valueType: DataType<K>) : DataType<M>(isNullable) TODO
//    abstract class Union<T> internal constructor(isNullable: Boolean, types: List<DataType<out T>>) : DataType<T>(isNullable) TODO
//    abstract class Struct<T> internal constructor(isNullable: Boolean, def: StructDef<T>) : DataType<T>(isNullable) TODO
//    Date is not supported because it's mutable and the most parts of it are deprecated 20+ years ago. TODO consider


}

inline fun <T, R> DataType<T>.match(simple: (isNullable: Boolean, DataType.Simple<T>) -> R): R =
        when (this) {
            is DataType.Nullable<*> -> when (actualType) {
                is DataType.Nullable<*> -> throw AssertionError()
                // Nullable<T?> wraps DataType<T> where T : Any
                // so, Nullable<T> wraps DataType<T!!> where T : Any?
                is DataType.Simple -> @Suppress("UNCHECKED_CAST") simple(true, actualType as DataType.Simple<T>)
            }
            is DataType.Simple -> simple(false, this)
        }
