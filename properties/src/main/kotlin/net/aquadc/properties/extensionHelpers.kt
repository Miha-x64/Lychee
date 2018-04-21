package net.aquadc.properties

//
// Boolean
//

@PublishedApi internal object UnaryNotBinaryAnd :
/* not: */ (Boolean) -> Boolean,
        /* and: */ (Boolean, Boolean) -> Boolean {

    override fun invoke(p1: Boolean): Boolean = !p1
    override fun invoke(p1: Boolean, p2: Boolean): Boolean = p1 && p2
}

@PublishedApi internal object Or : (Boolean, Boolean) -> Boolean {
    override fun invoke(p1: Boolean, p2: Boolean): Boolean = p1 || p2
}

@PublishedApi internal object Xor : (Boolean, Boolean) -> Boolean {
    override fun invoke(p1: Boolean, p2: Boolean): Boolean = p1 xor p2
}


//
// CharSequence
//

@PublishedApi internal object CharSeqLength : (CharSequence) -> Int {
    override fun invoke(p1: CharSequence): Int = p1.length
}

@PublishedApi internal object TrimmedCharSeq : (CharSequence) -> CharSequence {
    override fun invoke(p1: CharSequence): CharSequence = p1.trim()
}

@PublishedApi internal class CharSeqBooleanFunc(private val mode: Int) : (CharSequence) -> Boolean {
    override fun invoke(p1: CharSequence): Boolean = when (mode) {
        0 -> p1.isEmpty()
        1 -> p1.isNotEmpty()
        2 -> p1.isBlank()
        3 -> p1.isNotBlank()
        else -> throw AssertionError()
    }

    @PublishedApi
    internal companion object {
        @JvmField val Empty = CharSeqBooleanFunc(0)
        @JvmField val NotEmpty = CharSeqBooleanFunc(1)
        @JvmField val Blank = CharSeqBooleanFunc(2)
        @JvmField val NotBlank = CharSeqBooleanFunc(3)
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


//
// bulk
//

@PublishedApi internal class ContainsValue<in T>(
        private val value: T
) : (List<T>) -> Boolean {
    override fun invoke(p1: List<T>): Boolean = p1.contains(value)
}

@PublishedApi internal class ContainsAll<in T>(
        private val values: Collection<T>
) : (List<T>) -> Boolean {
    override fun invoke(p1: List<T>): Boolean = p1.containsAll(values)
}