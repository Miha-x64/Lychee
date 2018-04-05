package net.aquadc.properties


inline val Property<CharSequence>.length get() = map(CharSeqLength)

inline val Property<CharSequence>.isEmpty get() = map(CharSeqEmpty)
inline val Property<CharSequence>.isNotEmpty get() = map(CharSeqNotEmpty)

inline val Property<CharSequence>.isBlank get() = map(CharSeqBlank)
inline val Property<CharSequence>.isNotBlank get() = map(CharSeqNotBlank)

inline val Property<CharSequence>.trimmed get() = map(TrimmedCharSeq)

// if we'd just use lambdas, they'd be copied into call-site

@PublishedApi internal object CharSeqLength : (CharSequence) -> Int {
    override fun invoke(p1: CharSequence): Int = p1.length
}

@PublishedApi internal object TrimmedCharSeq : (CharSequence) -> CharSequence {
    override fun invoke(p1: CharSequence): CharSequence = p1.trim()
}

@PublishedApi internal val CharSeqEmpty = CharSeqBooleanFunc(0)
@PublishedApi internal val CharSeqNotEmpty = CharSeqBooleanFunc(1)
@PublishedApi internal val CharSeqBlank = CharSeqBooleanFunc(2)
@PublishedApi internal val CharSeqNotBlank = CharSeqBooleanFunc(3)
@PublishedApi internal class CharSeqBooleanFunc(private val mode: Int) : (CharSequence) -> Boolean {
    override fun invoke(p1: CharSequence): Boolean = when (mode) {
        0 -> p1.isEmpty()
        1 -> p1.isNotEmpty()
        2 -> p1.isBlank()
        3 -> p1.isNotBlank()
        else -> throw AssertionError()
    }
}
