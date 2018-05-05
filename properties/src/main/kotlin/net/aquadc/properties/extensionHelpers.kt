package net.aquadc.properties

import java.util.concurrent.atomic.AtomicBoolean

//
// Boolean
//

// I can't use erased types here,
// because checkcast to (Boolean, Boolean) -> Boolean
// would fail: https://youtrack.jetbrains.com/issue/KT-24067
@PublishedApi internal class BoolFunc(
        private val mode: Int
) :
        /* not: */ (Boolean) -> Boolean,
        /* and: */ (Boolean, Boolean) -> Boolean {

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
        @JvmField val And = BoolFunc(1)
        @JvmField val Or = BoolFunc(2)
        @JvmField val Xor = BoolFunc(3)
    }
}


//
// CharSequence
//

@PublishedApi internal class CharSeqFunc(private val mode: Int) : (Any) -> Any {
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
        @JvmField val Empty = CharSeqFunc(0) as (CharSequence) -> Boolean
        @JvmField val NotEmpty = CharSeqFunc(1) as (CharSequence) -> Boolean
        @JvmField val Blank = CharSeqFunc(2) as (CharSequence) -> Boolean
        @JvmField val NotBlank = CharSeqFunc(3) as (CharSequence) -> Boolean
        @JvmField val Length = CharSeqFunc(4) as (CharSequence) -> Int
        @JvmField val Trim = CharSeqFunc(5) as (CharSequence) -> CharSequence
    }
}


//
// contains
//

@Suppress("UNCHECKED_CAST") @PublishedApi
internal class Contains<T>(private val value: Any?, private val containsAll: Boolean) : (Any) -> Any? {

    override fun invoke(p1: Any): Any? {
        p1 as List<T>

        return if (containsAll) p1.containsAll(value as List<T>) else p1.contains(value as T)
    }

}

//
// common
//

@PublishedApi
internal object Just : (Any?) -> Any? {
    override fun invoke(p1: Any?): Any? = p1
}

/**
 * Compares objects by their identity.
 */
object Identity : (Any?, Any?) -> Boolean {
    override fun invoke(p1: Any?, p2: Any?): Boolean = p1 === p2
}

/**
 * Compares objects with [Any.equals] operator function.
 */
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
