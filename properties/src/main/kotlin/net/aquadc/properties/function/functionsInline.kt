@file:JvmName("FunctionsInline")
@file:Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
package net.aquadc.properties.function

// These methods are representing public way of getting functions.
// A typical function consists of getfield/getstatic + areturn,
// which means it's a great candidate for inlining.


/**
 * A function which returns its argument.
 * Analogous to [java.util.function.Function.identity].
 */
inline fun <T> identity(): (T) -> T =
        Identity as (T) -> T

/**
 * A function which compares its argument with [that],
 * i. e. it's a partially-applied version of [Objectz.Equal].
 */

inline fun isEqualTo(that: Any?): (Any?) -> Boolean =
        `AppliedFunc1-`(that, 3) as (Any?) -> Boolean


/**
 * A function which compares identity of its argument with [that],
 * i. e. it's a partially-applied version of [Objectz.NotEqual].
 */
inline fun notEqualTo(that: Any?): (Any?) -> Boolean =
        `AppliedFunc1-`(that, 5) as (Any?) -> Boolean

/**
 * A function which checks identity of its argument with [that],
 * i. e. it's a partially-applied version of [Objectz.Same].
 */
inline fun isSameAs(that: Any?): (Any?) -> Boolean =
        `AppliedFunc1-`(that, 4) as (Any?) -> Boolean

/**
 * A function which checks identity of its argument with [that] and inverts this value,
 * i. e. it's a partially-applied version of [Objectz.NotSame].
 */
inline fun notSameAs(that: Any?): (Any?) -> Boolean =
        `AppliedFunc1-`(that, 6) as (Any?) -> Boolean
