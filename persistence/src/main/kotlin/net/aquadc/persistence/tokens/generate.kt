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

suspend inline fun TokenStreamScope.yieldNull(compute: () -> Unit): Unit =
        offer(Token.Null).let { coerceTo -> if (coerceTo != false) {
            compute()
            yield((coerceTo as Token?).coerce(null))
        } }

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

    private val _path = TokenPath()
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
            afterToken(value)
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
            afterToken(it)
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
            afterToken(skipping) // skipped a closing brace, handle nesting change
        else
            afterValue(null) // skipped either a primitive or the whole object or array
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

    private val expectingName = ArrayList<Boolean?>()
    override suspend fun yield(value: Any?) {
        check(nextToken != null)
        check(yieldAll == null)
        nextValue = value
        return suspendCoroutineUninterceptedOrReturn { c ->
            nextOfferOrYieldAll = c
            COROUTINE_SUSPENDED
        }
    }

    private fun afterToken(value: Any?) {
        when (value) {
            Token.BeginSequence -> {
                check(expectingName.lastOrNull() != true) { "sequences as keys are unsupported" }
                _path.beginArray()
                expectingName.add(null)
            }
            Token.EndSequence -> {
                _path.endArray()
                check(expectingName.removeAt(expectingName.lastIndex) == null)
                flipExpectName()
            }
            Token.BeginDictionary -> {
                check(expectingName.lastOrNull() != true) { "dictionaries as keys are unsupported" }
                _path.beginObject()
                expectingName.add(true)
            }
            Token.EndDictionary -> {
                _path.endObject()
                check(expectingName.removeAt(expectingName.lastIndex) == true) {
                    "dangling name. Expected a value but was '${Token.EndDictionary}' at $path"
                }
                flipExpectName()
            }
            else -> {
                afterValue(value)
            }
        }
    }

    private fun afterValue(value: Any?) {
        if (expectingName.isNotEmpty()) {
            val en = expectingName.last()
            if (en == null) {
                _path.afterValue()
            } else {
                if (en) _path.onName(value)
                else _path.afterValue()
                expectingName[expectingName.lastIndex] = !en
            }
        } // else we're at the root element, nothing to do here
    }
    private fun flipExpectName() {
        if (expectingName.isNotEmpty()) {
            val li = expectingName.lastIndex
            expectingName[li]?.let { expectingName[li] = !it }
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
