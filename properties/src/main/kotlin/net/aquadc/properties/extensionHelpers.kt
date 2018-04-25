package net.aquadc.properties

import java.util.concurrent.atomic.AtomicBoolean

//
// Boolean
//

// I can't use erased types here,
// because checkcast to (Boolean, Boolean) -> Boolean
// would fail: https://youtrack.jetbrains.com/issue/KT-24067
@PublishedApi internal object UnaryNotBinaryAnd :
        /* not: */ (Boolean) -> Boolean,
        /* and: */ (Boolean, Boolean) -> Boolean {

    override fun invoke(p1: Boolean): Boolean = !p1
    override fun invoke(p1: Boolean, p2: Boolean): Boolean = p1 && p2
}

@PublishedApi internal object OrBooleans : (Any, Any) -> Any {
    override fun invoke(p1: Any, p2: Any): Any = p1 as Boolean || p2 as Boolean
}

@PublishedApi internal object XorBooleans : (Any, Any) -> Any {
    override fun invoke(p1: Any, p2: Any): Any = p1 as Boolean xor p2 as Boolean
}


//
// CharSequence
//

@PublishedApi internal object CharSeqLength : (Any) -> Any {
    override fun invoke(p1: Any): Any = (p1 as CharSequence).length
}

@PublishedApi internal object TrimmedCharSeq : (Any) -> Any {
    override fun invoke(p1: Any): Any = (p1 as CharSequence).trim()
}

@PublishedApi internal class CharSeqBooleanFunc(private val mode: Int) : (Any) -> Any {
    override fun invoke(p1: Any): Any {
        p1 as CharSequence
        return when (mode) {
            0 -> p1.isEmpty()
            1 -> p1.isNotEmpty()
            2 -> p1.isBlank()
            3 -> p1.isNotBlank()
            else -> throw AssertionError()
        }
    }

    @Suppress("UNCHECKED_CAST")
    @PublishedApi
    internal companion object {
        @JvmField val Empty = CharSeqBooleanFunc(0) as (CharSequence) -> Boolean
        @JvmField val NotEmpty = CharSeqBooleanFunc(1) as (CharSequence) -> Boolean
        @JvmField val Blank = CharSeqBooleanFunc(2) as (CharSequence) -> Boolean
        @JvmField val NotBlank = CharSeqBooleanFunc(3) as (CharSequence) -> Boolean
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
