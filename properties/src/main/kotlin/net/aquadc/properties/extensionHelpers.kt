package net.aquadc.properties

import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

//
// Boolean
//

// I can't use erased types here,
// because checkcast to (Boolean, Boolean) -> Boolean
// would fail: https://youtrack.jetbrains.com/issue/KT-24067
@PublishedApi internal class `BoolFunc-`(
        private val mode: Int
) :
        /* not: */ (Boolean) -> Boolean,
 /* and|or|xor: */ (Boolean, Boolean) -> Boolean {

    // When used as unary function, acts like 'not'.
    override fun invoke(p1: Boolean): Boolean = !p1

    override fun invoke(p1: Boolean, p2: Boolean): Boolean = when (mode) {
        1 -> p1 && p2
        2 -> p1 || p2
        3 -> p1 xor p2
        else -> throw AssertionError()
    }

    @Suppress("UNCHECKED_CAST")
    @PublishedApi
    internal companion object {
        @JvmField val And = `BoolFunc-`(1)
        @JvmField val Or = `BoolFunc-`(2)
        @JvmField val Xor = `BoolFunc-`(3)
    }
}

@PublishedApi internal class ToBoolFunc1(
        private val mode: Int
): (Any?) -> Boolean {

    override fun invoke(p1: Any?): Boolean = when (mode) {
        1 -> p1 === null
        2 -> p1 !== null
        3 -> (p1 as Collection<*>?)?.isEmpty() ?: true
        4 -> (p1 as Collection<*>?)?.isNotEmpty() ?: false
        else -> throw AssertionError()
    }

    @PublishedApi internal companion object {
        @JvmField val IsNull = ToBoolFunc1(1)
        @JvmField val IsNotNull = ToBoolFunc1(2)
        @JvmField val IsEmptyCollection: (Collection<*>?) -> Boolean = ToBoolFunc1(3)
        @JvmField val IsNonEmptyCollection: (Collection<*>?) -> Boolean = ToBoolFunc1(4)
    }

}

@PublishedApi internal class ToBoolFunc2(
        private val mode: Int
): (Any?, Any?) -> Boolean {

    override fun invoke(p1: Any?, p2: Any?): Boolean = when (mode) {
        1 -> p1 == p2
        2 -> p1 === p2
        3 -> Arrays.equals(p1 as BooleanArray, p2 as BooleanArray)
        4 -> Arrays.equals(p1 as ByteArray, p2 as ByteArray)
        5 -> Arrays.equals(p1 as ShortArray, p2 as ShortArray)
        6 -> Arrays.equals(p1 as CharArray, p2 as CharArray)
        7 -> Arrays.equals(p1 as IntArray, p2 as IntArray)
        8 -> Arrays.equals(p1 as LongArray, p2 as LongArray)
        9 -> Arrays.equals(p1 as FloatArray, p2 as FloatArray)
        10 -> Arrays.equals(p1 as DoubleArray, p2 as DoubleArray)
        11 -> Arrays.equals(p1 as Array<out Any?>, p2 as Array<out Any?>)
        12 -> Arrays.deepEquals(p1 as Array<out Any?>, p2 as Array<out Any?>)
        else -> throw AssertionError()
    }

    @PublishedApi internal companion object {
        @JvmField val Equality = ToBoolFunc2(1)
        @JvmField val Identity = ToBoolFunc2(2)
        @JvmField val Booleans = ToBoolFunc2(3) as (BooleanArray, BooleanArray) -> Boolean
        @JvmField val Bytes = ToBoolFunc2(4) as (ByteArray, ByteArray) -> Boolean
        @JvmField val Shorts = ToBoolFunc2(5) as (ShortArray, ShortArray) -> Boolean
        @JvmField val Chars = ToBoolFunc2(6) as (CharArray, CharArray) -> Boolean
        @JvmField val Ints = ToBoolFunc2(7) as (IntArray, IntArray) -> Boolean
        @JvmField val Longs = ToBoolFunc2(8) as (LongArray, LongArray) -> Boolean
        @JvmField val Floats = ToBoolFunc2(9) as (FloatArray, FloatArray) -> Boolean
        @JvmField val Doubles = ToBoolFunc2(10) as (DoubleArray, DoubleArray) -> Boolean
        @JvmField val Objects = ToBoolFunc2(11) as (Array<out Any?>, Array<out Any?>) -> Boolean
        @JvmField val ObjectsDeep = ToBoolFunc2(12) as (Array<out Any?>, Array<out Any?>) -> Boolean
    }

}


//
// CharSequence
//

@PublishedApi internal class `CharSeqFunc-`(private val mode: Int) : (Any) -> Any {
    override fun invoke(p1: Any): Any {
        p1 as CharSequence
        return when (mode) {
            0 -> p1.isEmpty()
            1 -> p1.isNotEmpty()
            2 -> p1.isBlank()
            3 -> p1.isNotBlank()
            4 -> p1.length
            5 -> p1.trim()
            else -> throw AssertionError()
        }
    }

    @Suppress("UNCHECKED_CAST")
    @PublishedApi
    internal companion object {
        @JvmField val Empty = `CharSeqFunc-`(0) as (CharSequence) -> Boolean
        @JvmField val NotEmpty = `CharSeqFunc-`(1) as (CharSequence) -> Boolean
        @JvmField val Blank = `CharSeqFunc-`(2) as (CharSequence) -> Boolean
        @JvmField val NotBlank = `CharSeqFunc-`(3) as (CharSequence) -> Boolean
        @JvmField val Length = `CharSeqFunc-`(4) as (CharSequence) -> Int
        @JvmField val Trim = `CharSeqFunc-`(5) as (CharSequence) -> CharSequence
    }
}


//
// contains
//

@Suppress("UNCHECKED_CAST") @PublishedApi
internal class `Contains-`<T>(
        private val value: Any?,
        private val mode: Int
) : (Any) -> Any? {

    override fun invoke(p1: Any): Any? = when (mode) {
        1 -> (p1 as Collection<T>).containsAll(value as List<T>)
        2 -> (p1 as Collection<T>).contains(value as T)
        3 -> p1 == value
        else -> throw AssertionError()
    }

}

//
// common
//

@PublishedApi
internal object Just : (Any?) -> Any? {
    override fun invoke(p1: Any?): Any? = p1
}

@PublishedApi
internal object ToPair : (Any?, Any?) -> Any? {

    // will be merged with any other class after Kotlin bug fix

    override fun invoke(p1: Any?, p2: Any?): Any? =
            Pair(p1, p2)
}

/**
 * Compares objects by their identity.
 */
@Deprecated("use 'byIdentity' function instead", ReplaceWith("byIdentity()"), DeprecationLevel.ERROR)
object Identity : (Any?, Any?) -> Boolean {
    override fun invoke(p1: Any?, p2: Any?): Boolean = p1 === p2
}

/**
 * Compares objects with [Any.equals] operator function.
 */
@Deprecated("use 'byEquality' function instead", ReplaceWith("byEquality()"), DeprecationLevel.ERROR)
object Equals : (Any?, Any?) -> Boolean {
    override fun invoke(p1: Any?, p2: Any?): Boolean = p1 == p2
}

@PublishedApi
internal abstract class OnEach<T> : ChangeListener<T>, (T) -> Unit {

    internal var calledRef: AtomicBoolean? = AtomicBoolean(false)

    override fun invoke(old: T, new: T) {
        calledRef?.let {
            it.set(true) // set eagerly, null out in lazy way
            calledRef = null
        }
        invoke(new)
    }

}
