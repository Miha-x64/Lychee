package net.aquadc.persistence.type

import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.FieldSet
import net.aquadc.persistence.struct.Schema

/**
 * The root of all types.
 * “Ilk” is just another synonym for “type” or “kind”.
 *
 * Used by :sql to use native types like `uuid`, `point` etc.
 */
interface Ilk<T, out DT : DataType<T>> {
    val type: DT
    val custom: CustomType<T>?
}

/**
 * Describes type of stored values and underlying serialization techniques.
 * This property is a part of serialization ABI.
 *
 * Replacing one DataType<T> with another DataType<T> (which is OK for source and binary compatibility)
 * may break serialization compatibility,
 * while replacing DataType<T1> with DataType<T2> (which may break source and binary compatibility)
 * may not, and vice versa.
 *
 * Data types are compatible if
 * * d1 is [DataType.NotNull.Simple] and d2 is [DataType.NotNull.Simple] and `d1.kind == d2.kind`
 * * d1 is [DataType.NotNull.Collect] and d2 is [DataType.NotNull.Collect] and d1.elementType is compatible with d2.elementType
 * * d1 is [DataType.NotNull.Partial] and d2 is [DataType.NotNull.Partial] and d1.schema is compatible to d2.schema
 *   (schemas s1 and s2 are considered to be compatible when they have the same number of fields,
 *    for each n s1.fields[n] has type compatible to s2.fields[n],
 *    and, depending on underlying serialization machinery,
 *    s1.fields[n] has either same name or same ordinal as s2.fields[n].)
 */
// Unfortunately, I can't implement Ilk by DataType: it would be impossible
// to narrow down DT parameter value in subclasses due to https://youtrack.jetbrains.com/issue/KT-13380.
// I could write @Suppress("INCONSISTENT_TYPE_PARAMETER_VALUES") but this would be required
// on every subclass including user-side `Schema`s which is horrible and inappropriate.
sealed class DataType<T> {

    // “accidental” override for Ilk.custom
    val custom: CustomType<T>? get() = null

    /**
     * Adds nullability to both runtime and stored representation of [actualType].
     *
     * The main reason why you can't have custom nullability is that
     * lens concatenation `toPartial / toSomething` or `toPartial / toSomethingNullable`
     * must produce predictable output for both runtime and stored representation.
     *
     * (However, some [NotNull.Simple], [NotNull.Collect], or [NotNull.Partial] implementations
     * may have nullable in-memory representation, and thus cannot be wrapped into [Nullable])
     */
    class Nullable<T : Any, DT : NotNull<T>>(
            /**
             * Wrapped non-nullable type.
             */
            @JvmField val actualType: DT
    ) : DataType<T?>(), Ilk<T?, Nullable<T, DT>> {

        init {
            if (actualType is Nullable<*, *>) throw ClassCastException() // unchecked cast?..
        }

        override val type: Nullable<T, DT> get() = this
    }

    // Unfortunately, I can't insert a supertype seamlessly.
    // Nesting required: https://youtrack.jetbrains.com/issue/KT-13495
    // There's also no local type aliases.

    /**
     * Common supertype for all types which cannot be stored as `null`.
     */
    sealed class NotNull<T> : DataType<T>() {

        /**
         * A simple, non-composite (and thus easily composable) type.
         */
        abstract class Simple<T>(
            /**
             * Specifies exact type of stored values.
             */
            @JvmField val kind: Kind
        ) : NotNull<T>(), Ilk<T, Simple<T>> {

            enum class Kind {
                Bool,
                I32, I64, // TODO: U32, U64, BigInt, BigFloat
                F32, F64,
                Str, Blob,
            }

            /**
             * If true, string-based serialization formats will call
             * [storeAsString] instead of [store], and will pass [CharSequence] to [load].
             * This is useful to override appearance of numbers or blobs in JSON, XML, etc.
             */
            open val hasStringRepresentation: Boolean get() = false

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

            /**
             * If [hasStringRepresentation], string-based serialization formats
             * will call this method instead of [store].
             */
            open fun storeAsString(value: T): CharSequence =
                throw UnsupportedOperationException()

            override val type: Simple<T> get() = this
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
        abstract class Collect<C, E, DE : DataType<E>>(
            /**
             * [DataType] of all the elements in such collections.
             */
            @JvmField val elementType: DE
        ) : NotNull<C>(), Ilk<C, Collect<C, E, DE>> {

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
             * @return persistable representation of [value], a collection of in-memory representations
             */
            abstract fun store(value: C): AnyCollection

            override val type: Collect<C, E, DE> get() = this
        }

        /**
         * Represents a set of optional key-value mappings, according to [schema].
         * [Schema] itself represents a special case of [Partial], where all mappings are required.
         */
        abstract class Partial<T, SCH : Schema<SCH>> : NotNull<T>, Ilk<T, Partial<T, SCH>> {

            @JvmField val schema: SCH

            constructor(schema: SCH) {
                this.schema = schema
            }

            internal constructor() { // for Schema itself
                this.schema = this as SCH
            }

            /**
             * Converts a persistable value into its in-memory representation.
             * @param fields a set of fields provided within [values] array
             * @param values values in their in-memory representation according to [fields] size
             *   0 -> ignored
             *   1 -> the value for the only field
             *   else -> 'packed' layout, no gaps between values
             * @return in-memory representation of [fields] and their [values]
             * @see net.aquadc.persistence.struct.indexOf
             * @see net.aquadc.persistence.fill
             */
            @Suppress("INAPPLICABLE_JVM_NAME") @JvmName("load") // make usable/overridable for/from Java
            abstract fun load(fields: FieldSet<SCH, FieldDef<SCH, *, *>>, values: Any?): T

            /**
             * Returns a set of fields which have values.
             * Required to parse data returned by [store] function.
             */
            abstract fun fields(value: T): FieldSet<SCH, FieldDef<SCH, *, *>>

            /**
             * Converts in-memory value into its persistable representation.
             * @param value an input value to read from
             * @return all values, using the same layouts as in [load], in their unchanged, in-memory representation
             * @see fields to know how to interpret the return value
             */
            abstract fun store(value: T): Any?

            override val type: Partial<T, SCH> get() = this
        }
    }

    // these look useless but help using assertEquals() in tests:

    final override fun equals(other: Any?): Boolean {
        if (other !is DataType<*> || javaClass !== other.javaClass) return false
        // class identity equality   ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ guarantees the same behaviour

        return when (this) {
            is Nullable<*, *> -> other is Nullable<*, *> && actualType as DataType<*> == other.actualType
            is NotNull.Simple -> other is NotNull.Simple<*> && kind === other.kind
            is NotNull.Collect<*, *, *> -> other is NotNull.Collect<*, *, *> && elementType == other.elementType
            is Schema<*> -> this === other
            is NotNull.Partial<*, *> -> other is NotNull.Partial<*, *> && schema == other.schema
        }
    }

    final override fun hashCode(): Int = when (this) {
        is Nullable<*, *> -> 13 * actualType.hashCode()
        is NotNull.Simple -> 31 * kind.hashCode()
        is NotNull.Collect<*, *, *> -> 63 * elementType.hashCode()
        is Schema<*> -> System.identityHashCode(this)
        is NotNull.Partial<*, *> -> 127 * schema.hashCode()
    }

    final override fun toString(): String = when (this) {
        is Nullable<*, *> -> "nullable($actualType)"
        is NotNull.Simple -> kind.toString()
        is NotNull.Collect<*, *, *> -> "collection($elementType)"
        is Schema<*> -> javaClass.simpleName
        is NotNull.Partial<*, *> -> "partial($schema)"
    }

}

/**
 * A custom type.
 * Used by :sql to take advantage of native types (e.g. Postgres `uuid`, `point` etc) directly.
 *
 * Very similar to `TwoWay` interface from :properties,
 * but :persistence don't have common dependency with :properties.
 */
abstract class CustomType<T>(
    @JvmField val name: CharSequence
) : (T) -> Any? {
    abstract fun back(p: Any?): T
}
