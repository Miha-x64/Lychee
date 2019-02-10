@file:JvmName("EnumTypes")
package net.aquadc.persistence.type

import java.util.*
import kotlin.collections.HashSet


// Enum


/**
 * Creates an [Enum] [DataType] implementation.
 * @param values all allowed values
 * @param encodeAs underlying data type
 * @param encode transform enum value [E] to underlying type [U]
 * @param fallback return a default value for unsupported [U] (or throw an exception, like default impl does)
 */
inline fun <reified E : Any, U : Any> enum(
        values: Array<E>,
        encodeAs: DataType.Simple<U>,
        noinline encode: (E) -> U,
        noinline fallback: (U) -> E = NoConstant(E::class.java) as (Any?) -> Nothing
): DataType.Simple<E> =
        enumInternal(values, encodeAs, encode, fallback)

/**
 * Special overload for the case when [E] is a real Java [Enum] type.
 * Finds an array of values automatically.
 */
inline fun <reified E : Enum<E>, U : Any> enum(
        encodeAs: DataType.Simple<U>,
        noinline encode: (E) -> U,
        noinline fallback: (U) -> E = NoConstant(E::class.java) as (Any?) -> Nothing
): DataType.Simple<E> =
        enumInternal(enumValues(), encodeAs, encode, fallback)

/**
 * Represents values of [E] type like [U] values.
 * [values] sample: `E.values()`
 * [encodeAs] sample: [string]
 * [encode] sample: `E::name`
 * [fallback] sample: `{ E.UNSUPPORTED }`
 */
@Suppress("UNCHECKED_CAST")
@PublishedApi internal fun <E : Any, U : Any> enumInternal(
        values: Array<E>,
        encodeAs: DataType.Simple<U>,
        encode: (E) -> U,
        fallback: (U) -> E
): DataType.Simple<E> =
        object : DataType.Simple<Any?>(encodeAs.kind) {

            private val lookup =
                    values.associateByTo(HashMap(values.size), encode).also { check(it.size == values.size) {
                        "there were duplicate names, check values of 'nameProp' for each enum constant passed in 'values'"
                    } }

            override fun decode(value: Any?): Any? {
                val u = encodeAs.decode(value)
                return lookup[u] ?: fallback(u)
            }

            override fun encode(value: Any?): Any =
                    encode.invoke(value as E)

        } as DataType.Simple<E>


// EnumSet


/**
 * Creates a [Set]<[Enum]> type implementation for storing enum set as a bitmask.
 * @param values all allowed values
 * @param encodeAs underlying data type
 * @param ordinal a getter for `values.indexOf(value)`
 */
inline fun <reified E> enumSet(
        values: Array<E>,
        encodeAs: DataType.Simple<Long>,
        noinline ordinal: (E) -> Int
): DataType.Simple<Set<E>> =
        enumSetInternal(E::class.java, values, encodeAs, ordinal)

/**
 * Creates a [Set]<[Enum]> type implementation for storing enum set as a bitmask.
 * Finds an array of values automatically.
 */
inline fun <reified E : Enum<E>> enumSet(
        encodeAs: DataType.Simple<Long>,
        noinline ordinal: (E) -> Int
): DataType.Simple<Set<E>> =
        enumSetInternal(E::class.java, enumValues(), encodeAs, ordinal)


@Suppress("UNCHECKED_CAST")
@PublishedApi internal fun <E> enumSetInternal(
        type: Class<E>,
        values: Array<E>,
        encodeAs: DataType.Simple<Long>,
        ordinal: (E) -> Int
): DataType.Simple<Set<E>> =
        object : DataType.Simple<Any?>(encodeAs.kind) {

            init {
                if (values.size > 64) throw UnsupportedOperationException("Enums with >64 values (JumboEnumSets) are not supported.")
            }

            override fun decode(value: Any?): Any? {
                var bitmask = encodeAs.decode(value)
                @Suppress("UPPER_BOUND_VIOLATED")
                val set: MutableSet<E> = if (type.isEnum) EnumSet.noneOf<E>(type) else HashSet() // TODO: use ArraySet if appropriate
                var ord = 0
                while (bitmask != 0L) {
                    if ((bitmask and 1L) == 1L) {
                        check(set.add(values[ord]))
                    }

                    bitmask = bitmask ushr 1
                    ord++
                }
                return set
            }

            override fun encode(value: Any?): Any? =
                    encodeAs.encode((value as Set<E>).fold(0L) { acc, e -> acc or (1L shl ordinal(e)) })

        } as DataType.Simple<Set<E>>

/**
 * Creates a [Set]<[Enum]> type implementation for storing enum set as a collection of values.
 * @param values all allowed values
 * @param encodeAs underlying element data type
 */
inline fun <reified E> enumSet(
        values: Array<E>,
        encodeAs: DataType<E>
): DataType.Collect<Set<E>, E> =
        enumSetInternal(E::class.java, values, encodeAs)

/**
 * Creates a [Set]<[Enum]> type implementation for storing enum set as a collection of values.
 * Finds an array of values automatically.
 */
inline fun <reified E : Enum<E>> enumSet(
        encodeAs: DataType<E>
): DataType.Collect<Set<E>, E> =
        enumSetInternal(E::class.java, enumValues(), encodeAs)

@PublishedApi internal fun <E> enumSetInternal(
        type: Class<E>,
        values: Array<E>,
        elementType: DataType<E>
): DataType.Collect<Set<E>, E> =
        object : DataType.Collect<Set<E>, E>(elementType) {

            override fun encode(value: Set<E>): Collection<Any?> =
                    value.map(elementType::encode)

            override fun decode(value: Collection<Any?>): Set<E> =
                    @Suppress("UPPER_BOUND_VIOLATED")
                    value.mapTo<Any?, E, Set<E>>(if (type.isEnum) EnumSet.noneOf<E>(type) else HashSet<E>(), elementType::decode)

        }


// Util


@PublishedApi internal class NoConstant(private val t: Class<*>) : (Any?) -> Any? {
    override fun invoke(p1: Any?): Any? {
        throw NoSuchElementException("No enum constant with name $p1 in type $t")
    }
}
