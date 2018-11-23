package net.aquadc.persistence.type


/**
 * Represents a way of storing a value.
 * 'Encoded' type is Any to avoid creating many different types inside a sealed class —
 * that would impede decorating existing [DataType] implementations.
 */
sealed class DataType<T>(
        @JvmField val isNullable: Boolean
) {

    /**
     * A simple, non-composite (and thus composable) type.
     */
    abstract class Simple<T>(
            isNullable: Boolean,
            val kind: Kind
    ) : DataType<T>(isNullable) {

        enum class Kind {
            Bool,
            I8, I16, I32, I64,
            F32, F64,
            /*TinyStr,*/ Str, /*BigStr,*/
            /*TinyBlob,*/ Blob, /*BigBlob*/
        }

        /**
         * @return [value] of type according to [kind]
         * @throws NullPointerException if [value] is null
         */
        abstract fun decode(value: Any): T

        /**
         * @return [value] as [T]
         * @throws NullPointerException if [value] is null
         */
        abstract fun encode(value: T): Any

    }

    /*abstract class Collect<C : Collection<T>?, T>(
            isNullable: Boolean,
            private val elementType: DataType.Simple<T>
    ) : DataType<C>(isNullable)

    abstract class StructCollection<SCH : StructDef<SCH>>(
            isNullable: Boolean,
            private val type: SCH
    ) : DataType<Collection<Struct<SCH>>>(isNullable)*/

//    abstract class Dictionary<M, K, V> internal constructor(isNullable: Boolean, keyType: DataType<K>, valueType: DataType<K>) : DataType<M>(isNullable) TODO
//    abstract class Union<T> internal constructor(isNullable: Boolean, types: List<DataType<out T>>) : DataType<T>(isNullable) TODO
//    abstract class Struct<T> internal constructor(isNullable: Boolean, def: StructDef<T>) : DataType<T>(isNullable) TODO
//    Date is not supported because it's mutable and the most parts of it are deprecated 20+ years ago. TODO consider


}
