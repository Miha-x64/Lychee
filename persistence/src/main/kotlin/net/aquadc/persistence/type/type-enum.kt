@file:JvmName("EnumTypes")
package net.aquadc.persistence.type

import androidx.annotation.RestrictTo
import net.aquadc.persistence.newMap
import java.util.EnumSet
import kotlin.collections.HashSet


// Enum


/**
 * Creates an [Enum] [DataType] implementation.
 * @param values all allowed values
 * @param encodeAs underlying data type
 * @param encode transform enum value [E] to underlying type [U]
 * @param fallback return a default value for unsupported [U] (or throw an exception, like default impl does)
 */
@Suppress("UNCHECKED_CAST") // NoConstant is intentionally erased
inline fun <reified E : Any, U : Any> enum(
    values: Array<out E>,
    encodeAs: DataType.NotNull.Simple<U>,
    noinline encode: (E) -> U,
    noinline fallback: (U) -> E = NoConstant(E::class.java) as (Any?) -> Nothing
): DataType.NotNull.Simple<E> =
        enumInternal(values, encodeAs, encode, fallback)

/**
 * Represents values of [E] type like [U] values.
 * [values] sample: `E.values()`
 * [encodeAs] sample: [string]
 * [encode] sample: `Enumz.Name`
 * [fallback] sample: `{ E.UNSUPPORTED }`
 */
@JvmName("enumeration") // Java-friendly, no reified
@Suppress("UNCHECKED_CAST")
fun <E : Any, U : Any> enumInternal(
    values: Array<out E>,
    encodeAs: DataType.NotNull.Simple<U>,
    encode: (E) -> U,
    fallback: (U) -> E
): DataType.NotNull.Simple<E> =
        object : DataType.NotNull.Simple<Any?>(encodeAs.kind) {

            private val lookup =
                    values.associateByTo(newMap(values.size), encode).also { check(it.size == values.size) {
                        "there were duplicate names, check values of 'nameProp' for each enum constant passed in 'values'"
                    } }

            override fun load(value: SimpleValue): Any? {
                val u = encodeAs.load(value)
                return lookup[u] ?: fallback(u)
            }

            override fun store(value: Any?): SimpleValue =
                    encodeAs.store(encode.invoke(value as E))

        } as DataType.NotNull.Simple<E>


// EnumSet


/**
 * Creates a [Set]<[Enum]> type implementation for storing enum set as a bitmask.
 * @param values all allowed values
 * @param encodeAs underlying data type
 * @param ordinal a getter for `values.indexOf(value)`
 */
inline fun <reified E> enumSet(
    values: Array<out E>,
    encodeAs: DataType.NotNull.Simple<Long>,
    noinline ordinal: (E) -> Int
): DataType.NotNull.Simple<Set<E>> =
        enumSetInternal(E::class.java, values, encodeAs, ordinal)

/**
 * Creates a [Set]<[Enum]> type implementation for storing enum set as a bitmask.
 * Finds an array of values automatically.
 */
inline fun <reified E : Enum<E>> enumSet(
    encodeAs: DataType.NotNull.Simple<Long>,
    noinline ordinal: (E) -> Int
): DataType.NotNull.Simple<Set<E>> =
        enumSetInternal(E::class.java, enumValues(), encodeAs, ordinal)


@Suppress("UNCHECKED_CAST")
@PublishedApi @JvmSynthetic internal fun <E> enumSetInternal(
    type: Class<E>,
    values: Array<out E>,
    encodeAs: DataType.NotNull.Simple<Long>,
    ordinal: (E) -> Int
): DataType.NotNull.Simple<Set<E>> =
        object : DataType.NotNull.Simple<Any?>(encodeAs.kind) {

            init {
                if (values.size > 64) throw UnsupportedOperationException("Enums with >64 values (JumboEnumSets) are not supported.")
            }

            override fun load(value: SimpleValue): Any? {
                var bitmask = encodeAs.load(value)
                @Suppress("UPPER_BOUND_VIOLATED")
                val set: MutableSet<E> = if (type.isEnum) EnumSet.noneOf<E>(type) else HashSet()
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

            override fun store(value: Any?): SimpleValue =
                    encodeAs.store((value as Set<E>).fold(0L) { acc, e -> acc or (1L shl ordinal(e)) })

        } as DataType.NotNull.Simple<Set<E>>

/**
 * Creates a [Set]<[Enum]> type implementation for storing enum set as a collection of values.
 * @param encodeAs underlying element data type
 */
inline fun <reified E, DE : DataType<E>> enumSet(
        encodeAs: DE
): DataType.NotNull.Collect<Set<E>, E, DE> =
        setInternal(encodeAs, E::class.java.takeIf { it.isEnum })


// Util


@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) class NoConstant(private val t: Class<*>) : (Any?) -> Any? {
    override fun invoke(p1: Any?): Any? { // avoid bridge method creation by erasing return type -^^^^
        throw NoSuchElementException("No enum constant $p1 in type $t")
    }
}
