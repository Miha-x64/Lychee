package net.aquadc.properties

import net.aquadc.properties.executor.PlatformExecutors
import java.util.*
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
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

@PublishedApi internal class `Functions1-`(
        private val mode: Int
): (Any?) -> Any {

    override fun invoke(p1: Any?): Any = when (mode) {
        1 -> (p1 as Collection<*>?)?.isEmpty() ?: true
        2 -> (p1 as Collection<*>?)?.isNotEmpty() ?: false
        3 -> (p1 as Collection<*>?)?.size ?: 0
        else -> throw AssertionError()
    }

    @Suppress("UNCHECKED_CAST")
    @PublishedApi internal companion object {
        @JvmField val IsEmptyCollection = `Functions1-`(1) as (Collection<*>?) -> Boolean
        @JvmField val IsNonEmptyCollection = `Functions1-`(2) as (Collection<*>?) -> Boolean
        @JvmField val Size = `Functions1-`(3) as (Collection<*>?) -> Int
    }

}

@PublishedApi internal class `Functions2-`(
        private val mode: Int
): (Any?, Any?) -> Any {

    override fun invoke(p1: Any?, p2: Any?): Any = when (mode) {
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
        13 -> p1 != p2
        14 -> p1 !== p2
        else -> throw AssertionError()
    }

    @Suppress("UNCHECKED_CAST") companion object {
        @JvmField val Equality = `Functions2-`(1) as (Any?, Any?) -> Boolean
        @JvmField val Identity = `Functions2-`(2) as (Any?, Any?) -> Boolean
        @JvmField val Booleans = `Functions2-`(3) as (BooleanArray, BooleanArray) -> Boolean
        @JvmField val Bytes = `Functions2-`(4) as (ByteArray, ByteArray) -> Boolean
        @JvmField val Shorts = `Functions2-`(5) as (ShortArray, ShortArray) -> Boolean
        @JvmField val Chars = `Functions2-`(6) as (CharArray, CharArray) -> Boolean
        @JvmField val Ints = `Functions2-`(7) as (IntArray, IntArray) -> Boolean
        @JvmField val Longs = `Functions2-`(8) as (LongArray, LongArray) -> Boolean
        @JvmField val Floats = `Functions2-`(9) as (FloatArray, FloatArray) -> Boolean
        @JvmField val Doubles = `Functions2-`(10) as (DoubleArray, DoubleArray) -> Boolean
        @JvmField val Objects = `Functions2-`(11) as (Array<out Any?>, Array<out Any?>) -> Boolean
        @JvmField val ObjectsDeep = `Functions2-`(12) as (Array<out Any?>, Array<out Any?>) -> Boolean
        @JvmField val Inequality = `Functions2-`(13) as (Any?, Any?) -> Boolean
        @JvmField val NonIdentity = `Functions2-`(14) as (Any?, Any?) -> Boolean
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

    @Suppress("UNCHECKED_CAST") companion object {
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
internal class `AppliedFunc1-`(
        private val value: Any?,
        private val mode: Int
) : (Any?) -> Any? {

    override fun invoke(p1: Any?): Any? = when (mode) {
        1 -> (p1 as Collection<*>).containsAll(value as Collection<*>)
        2 -> (p1 as Collection<*>).contains(value)
        3 -> p1 == value
        4 -> p1 === value
        5 -> p1 != value
        6 -> p1 !== value
        else -> throw AssertionError()
    }

    companion object {
        @JvmField val IsNull = `AppliedFunc1-`(null, 4) as (Any?) -> Boolean
        @JvmField val IsNotNull = `AppliedFunc1-`(null, 6) as (Any?) -> Boolean
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
@Deprecated("use 'areIdentical' function instead", ReplaceWith("areIdentical()"), DeprecationLevel.ERROR)
object Identity : (Any?, Any?) -> Boolean {
    override fun invoke(p1: Any?, p2: Any?): Boolean = p1 === p2
}

/**
 * Compares objects with [Any.equals] operator function.
 */
@Deprecated("use 'areEqual' function instead", ReplaceWith("areEqual()"), DeprecationLevel.ERROR)
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

@PublishedApi internal class Schedule(
        private val time: Long,
        private val unit: TimeUnit
) : (ScheduledExecutorService, Any?, Runnable) -> Unit {
    override fun invoke(p1: ScheduledExecutorService, p2: Any?, p3: Runnable) {
        val exec = PlatformExecutors.executorForCurrentThread()
        p1.schedule({ exec.execute(p3) }, time, unit)
    }
}

@PublishedApi internal class Arithmetic(
        private val mode: Int
) : (Any?, Any?) -> Any? {

    override fun invoke(p1: Any?, p2: Any?): Any? = when (mode) {
        1 -> plus(p1, p2)
        2 -> minus(p1, p2)
        3 -> times(p1, p2)
        4 -> div(p1, p2)
        else -> throw AssertionError()
    }

    private fun plus(p1: Any?, p2: Any?): Any? = when (p1) {
        is Int -> p1 + (p2 as Int)
        is Long -> p1 + (p2 as Long)
        is Float -> p1 + (p2 as Float)
        is Double -> p1 + (p2 as Double)
        else -> throw AssertionError()
    }

    private fun minus(p1: Any?, p2: Any?): Any? = when (p1) {
        is Int -> p1 - (p2 as Int)
        is Long -> p1 - (p2 as Long)
        is Float -> p1 - (p2 as Float)
        is Double -> p1 - (p2 as Double)
        else -> throw AssertionError()
    }

    private fun times(p1: Any?, p2: Any?): Any? = when (p1) {
        is Int -> p1 * (p2 as Int)
        is Long -> p1 * (p2 as Long)
        is Float -> p1 * (p2 as Float)
        is Double -> p1 * (p2 as Double)
        else -> throw AssertionError()
    }

    private fun div(p1: Any?, p2: Any?): Any? = when (p1) {
        is Int -> p1 / (p2 as Int)
        is Long -> p1 / (p2 as Long)
        is Float -> p1 / (p2 as Float)
        is Double -> p1 / (p2 as Double)
        else -> throw AssertionError()
    }

    companion object {
        val Plus = Arithmetic(1)
        val Minus = Arithmetic(2)
        val Times = Arithmetic(3)
        val Div = Arithmetic(4)
    }

}
