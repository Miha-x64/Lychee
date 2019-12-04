package net.aquadc.persistence.tokens

import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.RestrictsSuspension
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.createCoroutineUnintercepted
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import kotlin.coroutines.resume


/**
 * Create a [TokenStream] yielding values from a coroutine.
 * Creating valid stream is up to caller.
 * For example, dictionaries must have even number of values;
 * non-primitive keys are unsupported by transformations.
 */
fun tokens(block: suspend TokenStreamScope.() -> Unit): TokenStream =
        CoroutineTokenStream().also {
            it.nextOfferOrYieldAll = block.createCoroutineUnintercepted(receiver = it, completion = it)
        }

/**
 * Yield a `null` value.
 * @return whether value was consumed. `false` means it was skipped
 */
suspend inline fun TokenStreamScope.yieldNull(): Boolean =
        offer(Token.Null).let { coerceTo ->
            if (coerceTo != false) yield((coerceTo as Token?).coerce(null)).let { true }
            else false
        }

/**
 * Yield a [Boolean] value.
 * @return whether value was consumed. `false` means it was skipped
 */
suspend inline fun TokenStreamScope.yieldBool(compute: () -> Boolean): Boolean =
        offer(Token.Bool).let { coerceTo ->
            if (coerceTo != false) yield((coerceTo as Token?).coerce(compute())).let { true }
            else false
        }

// skipped I8 and I16, wanna get rid of these shallow types

/**
 * Yield an [Int] value.
 * @return whether value was consumed. `false` means it was skipped
 */
suspend inline fun TokenStreamScope.yieldInt(compute: () -> Int): Boolean =
        offer(Token.I32).let { coerceTo ->
            if (coerceTo != false) yield((coerceTo as Token?).coerce(compute())).let { true }
            else false
        }

/**
 * Yield a [Long] value.
 * @return whether value was consumed. `false` means it was skipped
 */
suspend inline fun TokenStreamScope.yieldLong(compute: () -> Long): Boolean =
        offer(Token.I64).let { coerceTo ->
            if (coerceTo != false) yield((coerceTo as Token?).coerce(compute())).let { true }
            else false
        }

/**
 * Yield a [Float] value.
 * @return whether value was consumed. `false` means it was skipped
 */
suspend inline fun TokenStreamScope.yieldFloat(compute: () -> Float): Boolean =
        offer(Token.F32).let { coerceTo ->
            if (coerceTo != false) yield((coerceTo as Token?).coerce(compute())).let { true }
            else false
        }

/**
 * Yield a [Double] value.
 * @return whether value was consumed. `false` means it was skipped
 */
suspend inline fun TokenStreamScope.yieldDouble(compute: () -> Double): Boolean =
        offer(Token.F64).let { coerceTo ->
            if (coerceTo != false) yield((coerceTo as Token?).coerce(compute())).let { true }
            else false
        }

/**
 * Yield a [String] value.
 * @return whether value was consumed. `false` means it was skipped
 */
suspend inline fun TokenStreamScope.yieldString(compute: () -> String): Boolean =
        offer(Token.Str).let { coerceTo ->
            if (coerceTo != false) yield((coerceTo as Token?).coerce(compute())).let { true }
            else false
        }

/**
 * Yield a [ByteArray] value.
 * @return whether value was consumed. `false` means it was skipped
 */
suspend inline fun TokenStreamScope.yieldBlob(compute: () -> ByteArray): Boolean =
        offer(Token.Blob).let { coerceTo ->
            if (coerceTo != false) yield((coerceTo as Token?).coerce(compute())).let { true }
            else false
        }

/**
 * Yield a sequence of values.
 * @return whether sequence was consumed. `false` means it was fully skipped
 */
suspend inline fun TokenStreamScope.yieldSequence(block: () -> Unit): Boolean =
        offer(Token.BeginSequence).let { coerceTo ->
            if (coerceTo != false) {
                yield((coerceTo as Token?).coerce(Token.BeginSequence))
                block()
                yieldEnd(Token.EndSequence)
                true
            } else false
        }

/**
 * Yield a dictionary, a sequence of name-value pairs.
 * @return whether dictionary was consumed. `false` means it was fully skipped
 */
suspend inline fun TokenStreamScope.yieldDictionary(block: () -> Unit): Boolean =
        offer(Token.BeginDictionary).let { coerceTo ->
            if (coerceTo != false) {
                yield((coerceTo as Token?).coerce(Token.BeginDictionary))
                block()
                yieldEnd(Token.EndDictionary)
                true
            } else false
        }

@PublishedApi internal suspend fun TokenStreamScope.yieldEnd(token: Token) {
    offer(token).let {
        if (it != false) yield((it as Token?).coerce(token))
    }
}


@RestrictsSuspension abstract class TokenStreamScope {
    @PublishedApi internal abstract suspend fun offer(token: Token): Any? // Token | null for coerceTo; false if skipped
    @PublishedApi internal abstract suspend fun yield(value: Any?)

    /**
     * Yield a whole value from [source] stream.
     * If next token is [Token.BeginSequence] or [Token.BeginDictionary],
     * this will yield collection contents and according [Token.EndSequence] or [Token.EndDictionary] token in the end.
     * @return whether value was consumed. `false` means it was fully skipped
     */
    abstract suspend fun yieldBracketSequence(source: TokenStream): Boolean

    /**
     * Yield the whole [source] stream until the end.
     */
    abstract suspend fun yieldAll(source: TokenStream)
}

private class CoroutineTokenStream : TokenStreamScope(), TokenStream, Continuation<Unit> {

    var nextOfferOrYieldAll: Continuation<Unit>? = null
    var nextYield: Continuation</* coerceTo: Token | null | false */ Any?>? = null

    var nextToken: Token? = null
    var nextValue: Any? = null

    var yieldAll: TokenStream? = null

    private val _path = NameTracingTokenPath()
    override val path: List<Any?>
        get() = _path

    override fun peek(): Token {
        yieldAll?.let { return it.peek() }

        nextToken?.let {
            return it // peek & offer were called some time ago
        }

        check(nextYield == null) // no nextToken -> poll was not called -> no one could set nextYield
        nextOfferOrYieldAll?.let {
            nextOfferOrYieldAll = null
            it.resume(Unit) // either calls our 'offer' or 'resumeWith' (completion)
        } ?: throw NoSuchElementException()

        return peek() // resume till next token or end of stream — we may resume several times if yieldAll(emptyStream) happened
    }

    override fun poll(coerceTo: Token?): Any? {
        if (yieldAll == null) peek() // recur until we find either yieldAll or yield

        yieldAll?.let {
            val value = it.poll(coerceTo)
            if (!it.hasNext()) yieldAll = null // free(consumedStream)
            _path.afterToken(value)
            return value
        }

        nextYield!!.let { // there was an offer(), go get the following yield()
            nextYield = null
            it.resume(coerceTo)
        }

        check(nextToken != null) // this could happen if 'offer' was called but there's no according 'yield'
        nextToken = null
        nextValue.let {
            nextValue = null
            _path.afterToken(it)
            return it
        }
    }

    override fun skipValue() {
        val skipping = peek()

        yieldAll?.let {
            it.skipValue()
            if (!it.hasNext()) yieldAll = null
        } ?: nextYield!!.let {
            nextYield = null
            nextToken = null
            it.resume(false)
        }

        if (skipping == Token.EndSequence || skipping == Token.EndDictionary)
            _path.afterToken(skipping) // skipped a closing brace, handle nesting change
        else
            _path.skip()
    }

    override fun hasNext(): Boolean {
        yieldAll?.let {
            check(it.hasNext())
            return true
        }

        nextToken?.let {
            return true // peek & offer were called some time ago
        }

        check(nextYield == null) // no nextToken -> poll was not called -> no one could set nextYield
        nextOfferOrYieldAll?.let {
            nextOfferOrYieldAll = null
            it.resume(Unit) // either calls our 'offer' or 'yieldAll' or 'resumeWith' (completion)
        } ?: return false

        return hasNext() // we could encounter several yieldAll(emptyStream)
    }

    // TokenStreamScope

    override suspend fun offer(token: Token): Any? {
        check(nextToken == null)
        check(yieldAll == null)
        nextToken = token
        return suspendCoroutineUninterceptedOrReturn { c ->
            nextYield = c
            COROUTINE_SUSPENDED
        }
    }

    override suspend fun yield(value: Any?) {
        check(nextToken != null)
        check(yieldAll == null)
        nextValue = value
        return suspendCoroutineUninterceptedOrReturn { c ->
            nextOfferOrYieldAll = c
            COROUTINE_SUSPENDED
        }
    }

    override suspend fun yieldBracketSequence(source: TokenStream): Boolean =
        offer(source.peek()).let { coerceTo ->
            if (coerceTo != false) yieldBracketSequence(source, source.poll(coerceTo as Token)).let { true }
            else source.skipValue().let { false }
        }

    private suspend fun yieldBracketSequence(source: TokenStream, token: Any?) {
        yield(token)
        when (token) {
            Token.BeginSequence -> {
                while (true) {
                    if (source.peek() == Token.EndSequence) break
                    else /*recur*/ yieldBracketSequence(source)
                }
                yieldEnd(Token.EndSequence)
            }
            Token.BeginDictionary -> {
                while (true) {
                    if (source.peek() == Token.EndDictionary) break
                    else /*recur*/yieldBracketSequence(source)
                }
                yieldEnd(Token.EndDictionary)
            }
            Token.EndSequence, Token.EndDictionary -> {
                throw IllegalArgumentException("unexpected token '$token', nesting problem at $path")
            }
            // else just ignore, nothing to do here
        }
    }

    override suspend fun yieldAll(source: TokenStream) {
        check(yieldAll == null)
        check(nextToken == null)
        check(nextOfferOrYieldAll == null)
        if (source.hasNext()) yieldAll = source // else skip empty stream
        return suspendCoroutineUninterceptedOrReturn { c ->
            nextOfferOrYieldAll = c
            COROUTINE_SUSPENDED
        }
    }

    // completion Continuation

    override val context: CoroutineContext
        get() = EmptyCoroutineContext

    override fun resumeWith(result: Result<Unit>) {
        result.getOrThrow() // just rethrow exception if it is there
        nextToken = null
    }

}
