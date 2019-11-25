package net.aquadc.persistence.tokens

import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.RestrictsSuspension
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.createCoroutineUnintercepted
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import kotlin.coroutines.resume


fun tokens(block: suspend TokenStreamScope.() -> Unit): TokenStream =
        CoroutineTokenStream().also {
            it.nextOfferOrYieldAll = block.createCoroutineUnintercepted(receiver = it, completion = it)
        }

suspend inline fun TokenStreamScope.yieldNull(): Unit =
        offer(Token.Null).let { coerceTo -> if (coerceTo != false) {
            yield((coerceTo as Token?).coerce(null))
        } }

suspend inline fun TokenStreamScope.yieldBool(compute: () -> Boolean): Unit =
        offer(Token.Bool).let { coerceTo -> if (coerceTo != false) {
            yield((coerceTo as Token?).coerce(compute()))
        } }

// skipped I8 and I16, wanna get rid of these shallow types

suspend inline fun TokenStreamScope.yieldInt(compute: () -> Int): Unit =
        offer(Token.I32).let { coerceTo -> if (coerceTo != false) {
            yield((coerceTo as Token?).coerce(compute()))
        } }

suspend inline fun TokenStreamScope.yieldLong(compute: () -> Long): Unit =
        offer(Token.I64).let { coerceTo -> if (coerceTo != false) {
            yield((coerceTo as Token?).coerce(compute()))
        } }

suspend inline fun TokenStreamScope.yieldFloat(compute: () -> Float): Unit =
        offer(Token.F32).let { coerceTo -> if (coerceTo != false) {
            yield((coerceTo as Token?).coerce(compute()))
        } }

suspend inline fun TokenStreamScope.yieldDouble(compute: () -> Double): Unit =
        offer(Token.F64).let { coerceTo -> if (coerceTo != false) {
            yield((coerceTo as Token?).coerce(compute()))
        } }

suspend inline fun TokenStreamScope.yieldString(compute: () -> String): Unit =
        offer(Token.Str).let { coerceTo -> if (coerceTo != false) {
            yield((coerceTo as Token?).coerce(compute()))
        } }

suspend inline fun TokenStreamScope.yieldBlob(compute: () -> ByteArray): Unit =
        offer(Token.Blob).let { coerceTo -> if (coerceTo != false) {
            yield((coerceTo as Token?).coerce(compute()))
        } }

suspend inline fun TokenStreamScope.yieldSequence(block: () -> Unit): Unit =
        offer(Token.BeginSequence).let { coerceTo -> if (coerceTo != false) {
            yield((coerceTo as Token?).coerce(Token.BeginSequence))
            block()
            offer(Token.EndSequence).let {
                if (it != false) yield((it as Token?).coerce(Token.EndSequence))
            }
        } }

suspend inline fun TokenStreamScope.yieldDictionary(block: () -> Unit): Unit =
        offer(Token.BeginDictionary).let { coerceTo -> if (coerceTo != false) {
            yield((coerceTo as Token?).coerce(Token.BeginDictionary))
            block()
            offer(Token.EndDictionary).let {
                if (it != false) yield((it as Token?).coerce(Token.EndDictionary))
            }
        } }


@RestrictsSuspension abstract class TokenStreamScope {
    @PublishedApi internal abstract suspend fun offer(token: Token): Any? // Token | null for coerceTo; false if skipped
    @PublishedApi internal abstract suspend fun yield(value: Any?)
    abstract suspend fun yieldAll(stream: TokenStream)
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

    override fun skip() {
        val skipping = peek()

        yieldAll?.let {
            it.skip()
            if (!it.hasNext()) yieldAll = null
        } ?: peek().also {
            nextYield!!.let {
                nextYield = null
                nextToken = null
                it.resume(false)
            }
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

    override suspend fun yieldAll(stream: TokenStream) {
        check(yieldAll == null)
        check(nextToken == null)
        check(nextOfferOrYieldAll == null)
        if (stream.hasNext()) yieldAll = stream // else skip empty stream
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
