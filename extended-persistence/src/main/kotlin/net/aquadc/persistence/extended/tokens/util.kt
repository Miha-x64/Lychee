package net.aquadc.persistence.extended.tokens

import net.aquadc.collections.contains
import net.aquadc.persistence.tokens.Index
import net.aquadc.persistence.tokens.NameTracingTokenPath
import net.aquadc.persistence.tokens.Token
import net.aquadc.persistence.tokens.TokenStream


internal abstract class Transform(
        @JvmField protected val source: TokenStream,
        @JvmField protected val pathMatcher: Array<Predicate>
) : TokenStream {

    @JvmField protected var _path: NameTracingTokenPath? = null
    final override val path: List<Any?>
        get() = _path ?: source.path

    protected fun matches(): Boolean {
        val path = source.path
        if (path.size != pathMatcher.size + 1) return false
        pathMatcher.forEachIndexed { idx, it ->
            val segment = path[idx]
            if (!it(if (segment is Index) segment.value else segment)) return false
            //                             fixme ^^^^^^ there's no way for caller to check whether we are within array
        }
        return true
    }

    protected fun copyPath(): NameTracingTokenPath =
            NameTracingTokenPath().also {
                it.addAll(source.path)
                _path = it
            }

    final override fun hasNext(): Boolean =
            source.hasNext()
    // we count on correct bracket sequences. Then, no matter which state we are in, this will be correct

}

internal fun TokenStream.writeBracketSequenceTo(buffer: MutableCollection<Any?>, token: Any?) {
    buffer.add(token)
    when (token) {
        Token.BeginSequence -> {
            while (true) {
                val next = poll()
                if (next == Token.EndSequence) break
                writeBracketSequenceTo(buffer, next)
            }
            buffer.add(Token.EndSequence)
        }
        Token.BeginDictionary -> {
            while (true) {
                val next = poll()
                if (next == Token.EndDictionary) break
                writeBracketSequenceTo(buffer, next)
            }
            buffer.add(Token.EndDictionary)
        }
        Token.EndSequence, Token.EndDictionary -> {
            throw IllegalArgumentException("unexpected token '$token', nesting problem at $path")
        }
        // else just ignore, nothing to do here
    }
}

internal fun TokenStream.pollValue(): Any? {
    val v = poll()
    return if (v is Token) {
        check(v in Token.ControlTokens)
        ArrayList<Any?>().also {
            writeBracketSequenceTo(it, v) // check for nesting problems is inside
        }
    } else v
}

internal fun Any?.checkName(): Any? {
    check(this !is Token) { "names of type '$this' are not supported" }
    return this
}
