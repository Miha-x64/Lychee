package net.aquadc.persistence.type

import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.FieldSet
import net.aquadc.persistence.struct.PartialStruct
import net.aquadc.persistence.struct.Schema


/**
 * Used by [DataType.Simple] and represents the following type, according to [DataType.Simple.Kind]:
 * [Boolean] | [Byte] | [Short] | [Int] | [Long] | [Float] | [Double] | [String] | [ByteArray]
 */
typealias SimpleValue = Any

/**
 * Used by [DataType.Collect] and represents the following type:
 * [Collection]<E> | [Array]<E> | EArray
 * where E represents [Byte], [Short], [Int], [Long], [Float], [Double],
 * EArray means [ByteArray], [ShortArray], [IntArray], [LongArray], [FloatArray], [DoubleArray] accordingly
 */
typealias AnyCollection = Any
// @see fatMap, fatMapTo, fatAsList, don't forget to update them

/**
 * Represents a way of storing a value.
 * 'Encoded' type is Any to avoid creating many different types inside a sealed class —
 * that would impede decorating existing [DataType] implementations.
 */
sealed class DataType<T> {

    /**
     * Adds nullability to runtime representation of [actualType].
     * Wraps only non-nullable type, represented as `null` in memory.
     * (However, some non-standard [Simple], [Collect], or [Partial] implementations
     * may have nullable in-memory representation, and thus cannot be wrapped into [Nullable])
     */
    class Nullable<T : Any>(
            /**
             * Wrapped non-nullable type.
             */
            val actualType: DataType<T>
    ) : DataType<T?>()

    /**
     * A simple, non-composite (and thus easily composable) type.
     */
    abstract class Simple<T>(
            /**
             * Specifies exact type of stored values.
             */
            val kind: Kind
    ) : DataType<T>() {

        enum class Kind {
            Bool,
            I8, I16, I32, I64,
            F32, F64,
            Str, Blob
        }

        /**
         * Converts a simple persistable value into its in-memory representation.
         * @return in-memory representation of [value]
         */
        abstract fun load(value: SimpleValue): T

        /**
         * Converts in-memory value into its simple persistable representation.
         * @return persistable representation of [value]
         */
        abstract fun store(value: T): SimpleValue

        // TODO: to & from String, e. g. to encode UUID as blob, but use string representation in JSON

    }

    /**
     * A collection of elements of [elementType].
     * In-memory type [C] is typically a collection, but it is not required.
     * May have [List] or [Set] semantics, depending on implementations
     * of both this data type and the underlying storage.
     *
     * Collection DataType handles only converting from/to a specified collection type,
     * leaving values untouched.
     */
    abstract class Collect<C, E>(
            /**
             * [DataType] of all the elements in such collections.
             */
            val elementType: DataType<E>
    ) : DataType<C>() {

        /**
         * Converts a persistable collection value into its in-memory representation.
         * Elements of input collection are already in their in-memory representation
         * @return in-memory representation of [value]
         */
        abstract fun load(value: AnyCollection): C

        /**
         * Converts in-memory value into a persistable collection.
         * Values of output collection must be in their in-memory representation,
         * it's caller's responsibility to convert them to persistable representation.
         * @return persistable representation of [value]
         */
        abstract fun store(value: C): AnyCollection

    }

    /**
     * Represents a set of optional key-value mappings, according to [schema].
     * [Schema] itself represents a special case of [Partial], where all mappings are required.
     */
    abstract class Partial<T, SCH : Schema<SCH>> : DataType<T>() {

        abstract val schema: SCH

        /**
         * Converts a persistable value into its in-memory representation.
         * @param fields a set of fields provided within [values] array
         * @param values all values for [fields] listed, with gaps for absent values,
         *     so `values[field.ordinal]` is a valid value for a field; `null` is allowed only if [fields] are empty
         * @return in-memory representation of data in [values]
         * @see net.aquadc.persistence.struct.forEachIndexed function to iterate over a [FieldSet] in natural order
         */
        abstract fun load(fields: FieldSet<SCH, FieldDef<SCH, *>>, values: Array<Any?>?): T

        /**
         * Converts in-memory value into its persistable representation.
         * @return persistable representation of [value]
         */
        abstract fun store(value: T): PartialStruct<SCH>

    }


//    abstract class Dictionary<M, K, V> internal constructor(isNullable: Boolean, keyType: DataType<K>, valueType: DataType<K>) : DataType<M>(isNullable) TODO
//    abstract class Union<T> internal constructor(isNullable: Boolean, types: List<DataType<out T>>) : DataType<T>(isNullable) TODO
//    abstract class Struct<T> internal constructor(isNullable: Boolean, def: StructDef<T>) : DataType<T>(isNullable) TODO

//    TODO: date, time, UUID, etc types in a separate module

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
                is DataType.Simple -> payload.simple(arg, true, actualType)
                is DataType.Collect<*, *> -> payload.collection(arg, true, actualType as DataType.Collect<T, *>)
                is DataType.Partial<T, *> -> @Suppress("UPPER_BOUND_VIOLATED")
                        payload.partial<Schema<*>>(arg, true, actualType as DataType.Partial<T, Schema<*>>)
            }
        }
        is DataType.Simple -> payload.simple(arg, false, dataType)
        is DataType.Collect<T, *> -> payload.collection(arg, false, dataType)
        is DataType.Partial<T, *> -> @Suppress("UPPER_BOUND_VIOLATED")
                payload.partial<Schema<*>>(arg, false, dataType as DataType.Partial<T, Schema<*>>)
    }

interface DataTypeVisitor<PL, ARG, T, R> {
    fun PL.simple(arg: ARG, nullable: Boolean, type: DataType.Simple<T>): R
    fun <E> PL.collection(arg: ARG, nullable: Boolean, type: DataType.Collect<T, E>): R
    fun <SCH : Schema<SCH>> PL.partial(arg: ARG, nullable: Boolean, type: DataType.Partial<T, SCH>): R
}
