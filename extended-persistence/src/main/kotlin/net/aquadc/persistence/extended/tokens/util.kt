package net.aquadc.persistence.extended.tokens

import net.aquadc.collections.contains
import net.aquadc.persistence.tokens.Token
import net.aquadc.persistence.tokens.TokenStream


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
        ArrayList<Any?>().also { writeBracketSequenceTo(it, v) }
    } else v
}