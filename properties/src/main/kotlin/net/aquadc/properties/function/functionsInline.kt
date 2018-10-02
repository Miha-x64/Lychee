@file:JvmName("FunctionsInline")
@file:Suppress("NOTHING_TO_INLINE")
package net.aquadc.properties.function

// These methods are representing public way of getting functions.
// A typical function consists of getfield/getstatic + areturn,
// which means it's a great candidate for inlining.


/**
 * A function which returns its argument.
 * Analogous to [java.util.function.Function.identity].
 */
@Suppress("UNCHECKED_CAST")
inline fun <T> identity(): (T) -> T =
        Just as (T) -> T

/**
 * A function which compares the given arguments using [Object.equals] operator function.
 */
inline fun areEqual(): (Any?, Any?) -> Boolean =
        `Functions2-`.Equality

/**
 * A function which compares the given arguments using inverted return value of [Object.equals] operator function.
 */
inline fun areNotEqual(): (Any?, Any?) -> Boolean =
        `Functions2-`.Inequality

/**
 * A function which compares the given arguments by identity.
 */
inline fun areIdentical(): (Any?, Any?) -> Boolean =
        `Functions2-`.Identity

/**
 * A function which compares given arguments by identity and inverts this value.
 */
inline fun areNotIdentical(): (Any?, Any?) -> Boolean =
        `Functions2-`.NonIdentity

/**
 * A function which compares its argument with [that],
 * i. e. it's a partially-applied version of [areEqual].
 */
@Suppress("UNCHECKED_CAST") // using 'Any?' return type to avoid bridge method generation
inline fun isEqualTo(that: Any?): (Any?) -> Boolean =
        `AppliedFunc1-`(that, 3) as (Any?) -> Boolean


/**
 * A function which compares identity of its argument with [that],
 * i. e. it's a partially-applied version of [areNotEqual].
 */
@Suppress("UNCHECKED_CAST") // using 'Any?' return type to avoid bridge method generation
inline fun notEqualTo(that: Any?): (Any?) -> Boolean =
        `AppliedFunc1-`(that, 5) as (Any?) -> Boolean

/**
 * A function which checks identity of its argument with [that],
 * i. e. it's a partially-applied version of [areIdentical].
 */
@Suppress("UNCHECKED_CAST") // using 'Any?' return type to avoid bridge method generation
inline fun isIdenticalTo(that: Any?): (Any?) -> Boolean =
        `AppliedFunc1-`(that, 4) as (Any?) -> Boolean

/**
 * A function which checks identity of its argument with [that] and inverts this value,
 * i. e. it's a partially-applied version of [areNotIdentical].
 */
@Suppress("UNCHECKED_CAST") // using 'Any?' return type to avoid bridge method generation
inline fun notIdenticalTo(that: Any?): (Any?) -> Boolean =
        `AppliedFunc1-`(that, 6) as (Any?) -> Boolean

/**
 * A function which checks whether its argument is equal to `null`,
 * i. e. it's a partially-applied version of [identicalTo].
 */
@Suppress("UNCHECKED_CAST") // using 'Any?' return type to avoid bridge method generation
inline fun isNull(): (Any?) -> Boolean =
        `AppliedFunc1-`.IsNull

/**
 * A function which checks whether its argument is not equal to `null`,
 * i. e. it's a partially-applied version of [notIdenticalTo].
 */
@Suppress("UNCHECKED_CAST") // using 'Any?' return type to avoid bridge method generation
inline fun isNotNull(): (Any?) -> Boolean =
        `AppliedFunc1-`.IsNotNull

/**
 * A function which [Boolean] arrays with [java.util.Arrays.equals].
 */
inline fun areBoolArraysEqual(): (BooleanArray, BooleanArray) -> Boolean =
        `Functions2-`.Booleans

/**
 * A function which [Byte] arrays with [java.util.Arrays.equals].
 */
inline fun areByteArraysAreEqual(): (ByteArray, ByteArray) -> Boolean =
        `Functions2-`.Bytes

/**
 * A function which [Short] arrays with [java.util.Arrays.equals].
 */
inline fun areShortArraysAreEqual(): (ShortArray, ShortArray) -> Boolean =
        `Functions2-`.Shorts

/**
 * A function which [Char] arrays with [java.util.Arrays.equals].
 */
inline fun areCharArraysAreEqual(): (CharArray, CharArray) -> Boolean =
        `Functions2-`.Chars

/**
 * A function which [Int] arrays with [java.util.Arrays.equals].
 */
inline fun areIntArraysAreEqual(): (IntArray, IntArray) -> Boolean =
        `Functions2-`.Ints

/**
 * A function which [Long] arrays with [java.util.Arrays.equals].
 */
inline fun areLongArraysAreEqual(): (LongArray, LongArray) -> Boolean =
        `Functions2-`.Longs

/**
 * A function which [Float] arrays with [java.util.Arrays.equals].
 */
inline fun areFloatArraysAreEqual(): (FloatArray, FloatArray) -> Boolean =
        `Functions2-`.Floats

/**
 * A function which [Double] arrays with [java.util.Arrays.equals].
 */
inline fun areDoubleArraysAreEqual(): (DoubleArray, DoubleArray) -> Boolean =
        `Functions2-`.Doubles

/**
 * A function which reference arrays with [java.util.Arrays.equals].
 */
inline fun areArraysEqual(): (Array<out Any?>, Array<out Any?>) -> Boolean =
        `Functions2-`.Objects

/**
 * A function which reference arrays with [java.util.Arrays.deepEquals].
 */
inline fun areArraysDeeplyEqual(): (Array<out Any?>, Array<out Any?>) -> Boolean =
        `Functions2-`.ObjectsDeep

//
// CharSequence
//

/**
 * A function which returns the result of invocation [CharSequence.length] on its argument.
 */
inline fun length(): (CharSequence) -> Int =
        `CharSeqFunc-`.Length

/**
 * A function which returns the result of invocation [CharSequence.isEmpty] on its argument.
 */
inline fun isEmptyCharSequence(): (CharSequence) -> Boolean =
        `CharSeqFunc-`.Empty

/**
 * A function which returns the result of invocation [CharSequence.isNotEmpty] on its argument.
 */
inline fun isNonEmptyCharSequence(): (CharSequence) -> Boolean
        = `CharSeqFunc-`.NotEmpty

/**
 * A function which returns the result of invocation [CharSequence.isBlank] on its argument.
 */
inline fun isBlank(): (CharSequence) -> Boolean
        = `CharSeqFunc-`.Blank

/**
 * A function which returns the result of invocation [CharSequence.isNotBlank] on its argument.
 */
inline fun isNotBlank(): (CharSequence) -> Boolean
        = `CharSeqFunc-`.NotBlank

/**
 * A function which returns the result of invocation [CharSequence.trim] on its argument.
 */
inline fun trimmed(): (CharSequence) -> CharSequence
        = `CharSeqFunc-`.Trim

//
// Collection
//

/**
 * A function which returns the result of invocation [Collection.size] on its argument.
 */
inline fun size(): (Collection<*>) -> Int =
        `Functions1-`.Size

/**
 * A function which returns the result of invocation [Collection.isEmpty] on its argument.
 */
inline fun isEmptyCollection(): (Collection<*>?) -> Boolean =
        `Functions1-`.IsEmptyCollection


/**
 * A function which returns the result of invocation [Collection.isNotEmpty] on its argument.
 */
inline fun isNonEmptyCollection(): (Collection<*>?) -> Boolean =
        `Functions1-`.IsNonEmptyCollection


//
// Help
//

/**
 * Helps finding out necessary functions.
 * @see isEmptyCharSequence
 * @see isEmptyCollection
 */
@Deprecated("Use either isEmptyCharSequence() or isEmptyCollection().", level = DeprecationLevel.ERROR)
inline fun isEmpty(): Nothing =
        throw AssertionError()


/**
 * Helps finding out necessary functions.
 * @see isNonEmptyCharSequence
 * @see isNonEmptyCollection
 */
@Deprecated("Use either isNonEmptyCharSequence() or isNonEmptyCollection().", level = DeprecationLevel.ERROR)
inline fun isNotEmpty(): Nothing =
        throw AssertionError()
