package net.aquadc.persistence.extended.tokens

import net.aquadc.collections.contains
import net.aquadc.persistence.tokens.coerce
import net.aquadc.persistence.tokens.Index
import net.aquadc.persistence.tokens.NameTracingTokenPath
import net.aquadc.persistence.tokens.Token
import net.aquadc.persistence.tokens.TokenStream


@PublishedApi internal class AssociateTokens(
        source: TokenStream,
        pathMatcher: Array<Predicate>,
        nameKey: Any?,
        valueKey: Any?
) : AssociateEntries(source, pathMatcher, nameKey, valueKey) {

    private var state = -4
    private val valueBuffer = ArrayList<Any?>()

    override fun peek(): Token = when (state) {
        // nothing special, maybe standing before our BeginDictionary
        -4 -> {
            val token = source.peek()
            if (token == Token.BeginSequence && matches(0)) Token.BeginDictionary else token
        }

        // standing in the beginning; unreachable for [name, value] version
        -3 -> approachName().let { source.peek() }

        // standing right before name
        -2 -> source.peek()

        // standing somewhere before value; see #1 for reachability
        -1 -> approachValue().let { peek(/* either from buffer or from source */) }

        // the end, all mappings have been read
        Int.MAX_VALUE -> Token.EndDictionary

        else -> when (valueBuffer.size) {
            // standing right before value which was not buffered
            0 -> source.peek()

            // reading the buffer
            else -> valueBuffer[state].let { Token.ofValue(it) ?: throw IllegalArgumentException("bad value: $it") }
        }
    }

    override fun poll(coerceTo: Token?): Any? = when (state) {
        -4 -> {
            // we represent sequences as dictionaries, request sequence from delegate if dictionary requested
            val token = source.poll(if (coerceTo == Token.BeginDictionary) Token.BeginSequence else coerceTo)
            if (token == Token.BeginSequence && matches(1)) { // we've polled BeginSequence, no we're inside it, so plusNesting = 1
                copyPath().afterToken(Token.BeginDictionary)
                state = if (source.peek() == Token.EndSequence) {
                    1 // the end! Empty sequence -> empty dictionary
                } else {
                    source.poll(beginWrap) // remove nested wrapper right now, we ain't gonna need it
                    when (nameFirstInTuple) {
                        true -> -2
                        false -> -3
                        null -> -3
                    }
                }

                Token.BeginDictionary
            } else {
                token
            }
        }
        -3 -> {
            approachName()
            poll(coerceTo)
        }
        -2 -> {
            state = when (nameFirstInTuple) {
                true -> 0
                false -> 0 // value is buffered, just return it when asked to
                null -> -1 // needs to be searched for
            }
            source.poll(coerceTo).also(_path!!::afterToken)
        }
        -1 -> {
            approachValue()
            poll(coerceTo)
        }
        Int.MAX_VALUE -> {
            state = -4
            _path = null
            coerceTo.coerce(Token.EndDictionary)
        }
        else -> when (valueBuffer.size) {
            0 -> {
                val value = source.poll(coerceTo)
                _path!!.afterToken(value)
                if (_path!!.size == pathMatcher.size + 1) { // path of interest . name
                    // we're outside 'value', this is either beginning or end of 'value'
                    if (value !is Token || value.let { it == Token.EndSequence || it == Token.EndDictionary }) {
                        exitMapping() // gonna advance to be either before new mapping or in the end of the sequence
                    } // else give out nesting inside value as is
                } // else just continue giving out value tokens
                value
            }
            else -> {
                val valueBuf = valueBuffer
                val value = valueBuf.set(state++, null)
                if (valueBuf.size == state) {
                    exitMapping()
                }
                coerceTo.coerce(value.also(_path!!::afterToken))
            }
        }
    }

    override fun skipValue(): Unit = when (state) {
        -4 -> {
            source.skipValue()
        }
        -3 -> {
            approachName()
            source.skipValue()
            _path!!.skip()
            state = -1
        }
        -2 -> {
            source.skipValue()
            _path!!.skip()
            state = when (nameFirstInTuple) {
                true -> 0
                false -> 0
                null -> -1
            }
        }
        -1 -> {
            when (nameFirstInTuple) {
                true -> throw AssertionError()
                false -> throw AssertionError() // the value was already buffered
                null -> approachValue().also { skipValue() }
            }
        }
        Int.MAX_VALUE -> {
            state = -4
            _path = null
        }
        else -> when (valueBuffer.size) {
            0 -> {
                val token = source.peek()
                source.skipValue()

                // pass nesting change to path
                if (token.let { it == Token.EndSequence || it == Token.EndDictionary }) _path!!.afterToken(token)
                else _path!!.skip()

                if (_path!!.size == pathMatcher.size + 1) {
                    exitMapping()
                }

                Unit
            }
            else -> {
                if (state == 0) { // the beginning of our buffer, skip it and whole bracket sequence stored
                    _path!!.skip()
                    exitMapping()
                } else {
                    super.skipValue()
                }
            }
        }
    }

    private fun approachName() {
        state = -2
        return when (nameFirstInTuple) {
            true -> throw AssertionError()
            false -> {
                source.writeBracketSequenceTo(valueBuffer, source.poll())
            }
            null -> loop@ while (true) {
                when (val name = source.poll()) {
                    nameKey ->
                        break@loop // yay!

                    valueKey ->
                        source.writeBracketSequenceTo(valueBuffer, source.poll())

                    endWrap ->
                        throw IllegalArgumentException("required '$nameKey' was not found at ${source.path}")

                    is Token ->
                        throw IllegalArgumentException("unexpected token '$name', nesting problem at ${source.path}")

                    else ->
                        source.skipValue() // TODO should we fail here?
                }
            }
        }
    }

    private fun approachValue() {
        state = 0
        return when (nameFirstInTuple) {
            true -> throw AssertionError()
            false -> throw AssertionError()
            null -> loop@ while (true) {
                when (val name = source.poll()) {
                    valueKey ->
                        break@loop // yay!

                    endWrap ->
                        throw IllegalArgumentException("required '$nameKey' was not found at ${source.path}")

                    is Token ->
                        throw IllegalArgumentException("unexpected token '$name', nesting problem at ${source.path}")

                    else ->
                        source.skipValue() // TODO should we fail here?
                }
            }
        }
    }

    private fun exitMapping() {
        valueBuffer.clear()

        while (true) {
            val next = source.poll()
            if (next == endWrap) break
            next.checkName()

            // TODO should we fail here?
            if (nameFirstInTuple == null) source.skipValue() // skipped a name, skip according value then
        }

        // endWrap is consumed now, we're standing either before next mapping or the end of sequence

        state = when (val next = source.poll()) {
            beginWrap -> // next key-value pair
                when (nameFirstInTuple) {
                    true -> -2
                    false -> -3
                    null -> -3
                }

            Token.EndSequence -> // the end
                Int.MAX_VALUE

            else ->
                throw IllegalArgumentException("expected '$beginWrap' or ${Token.EndSequence} but was $next at ${source.path}")
        }
    }

    override fun copyPath(): NameTracingTokenPath = // pop BeginSequence, we're representing sequences as dictionaries
            super.copyPath().also {
                check(it.removeAt(it.lastIndex) is Index) // don't mind expectingName state, we don't use its outer part
            }

}

@PublishedApi internal class DissociateTokens(
        source: TokenStream, pathMatcher: Array<Predicate>, nameKey: Any?, valueKey: Any?
) : AssociateEntries(source, pathMatcher, nameKey, valueKey) {

    private var state = 0
    private var bufferedNameToken: Token? = null
    private var bufferedName: Any? = null

    override fun peek(): Token = when (state) {

        // nothing interesting here, just looking for BeginDictionary and an opportunity to emit our BeginSequence
        0 -> {
            val token = source.peek()
            if (token == Token.BeginDictionary && matches(0)) Token.BeginSequence else token
        }

        // standing before key, gonna emit beginWrap
        1 -> beginWrap

        // standing before key, gonna emit its name
        2 -> Token.ofValue(nameKey) ?: throw IllegalArgumentException("bad value: $nameKey")

        // standing before key and gonna emit either key or value, depending on nameFirstInTuple
        3 -> when (nameFirstInTuple) {
            true -> source.peek()
            false -> {
                if (bufferedNameToken == null) bufferName()
                source.peek()
            }
            null -> source.peek()
        }

        // standing before value, gonna emit its name
        4 -> Token.ofValue(valueKey) ?: throw IllegalArgumentException("bad value: $valueKey")

        // standing either before value and gonna emit it, or after value and gonna emit buffered key
        5 -> when (nameFirstInTuple) {
            true -> source.peek()
            false -> bufferedNameToken!!
            null -> source.peek()
        }

        // standing after value and gonna emit endWrap
        6 -> endWrap

        // the end, gonna emit EndSequence
        7 -> Token.EndSequence

        else -> throw AssertionError()
    }

    override fun poll(coerceTo: Token?): Any? = when (state) {
        0 -> {
            if (matches(0)) {
                val token = source.poll(if (coerceTo == Token.BeginSequence) Token.BeginDictionary else coerceTo)
                if (token == Token.BeginDictionary) {
                    copyPath().afterToken(Token.BeginSequence)
                    state = if (source.peek() == Token.EndDictionary) 7 else 1

                    Token.BeginSequence
                } else {
                    token
                }
            } else {
                source.poll(coerceTo)
            }
        }

        1 -> {
            state = when (nameFirstInTuple) {
                true -> 3
                false -> 3
                null -> 2
            }
            coerceTo.coerce(beginWrap).also(_path!!::afterToken)
        }

        2 -> {
            state = 3
            coerceTo.coerce(nameKey).also(_path!!::afterToken)
        }

        3 -> when (nameFirstInTuple) {
            true -> {
                state = 5
                source.poll(coerceTo).also(_path!!::afterToken)
            }
            false -> {
                if (bufferedNameToken == null) bufferName()
                emitValueToken(coerceTo, 5)
            }
            null -> {
                state = 4
                source.poll(coerceTo).also(_path!!::afterToken)
            }
        }

        4 -> {
            state = 5
            coerceTo.coerce(valueKey).also(_path!!::afterToken)
        }

        5 -> when (nameFirstInTuple) {
            true -> {
                emitValueToken(coerceTo, 6)
            }
            false -> {
                check(bufferedNameToken != null)
                state = 6
                coerceTo.coerce(bufferedName).also {
                    bufferedName = null
                    bufferedNameToken = null
                    _path!!.afterToken(it)
                }
            }
            null -> {
                emitValueToken(coerceTo, 6)
            }
        }

        6 -> {
            state = if (source.peek() == Token.EndDictionary) 7 else 1
            coerceTo.coerce(endWrap).also(_path!!::afterToken)
        }

        7 -> {
            state = 0
            source.poll(Token.EndDictionary)
            coerceTo.coerce(Token.EndSequence).also(_path!!::afterToken)
        }

        else -> {
            throw AssertionError()
        }
    }

    override fun skipValue(): Unit = when (state) {
        0 -> {
            source.skipValue()
        }
        1 -> { // skipping our beginWrap, i. e. the whole dictionary/tuple
            source.skipValue()
            source.skipValue()
            _path!!.skip()
            if (source.peek() == Token.EndDictionary) state = 7 // skipped last entry
            Unit
        }
        2 -> {
            _path!!.afterToken(nameKey)
            state = 3
        }
        3 -> {
            state = when (nameFirstInTuple) {
                true -> 5
                false -> 5.also { if (bufferedNameToken == null) bufferName() }
                null -> 4
            }
            _path!!.skip()
            source.skipValue()
        }
        4 -> {
            _path!!.afterToken(valueKey)
            state = 5
        }
        5 -> {
            when (nameFirstInTuple) {
                true -> {
                    skipValueToken(6)
                }
                false -> {
                    bufferedName.let {
                        bufferedName = null
                        bufferedNameToken = null
                        _path!!.afterToken(it)
                    }
                    state = 6
                }
                null -> {
                    skipValueToken(6)
                }
            }
        }
        6 -> {
            state = if (source.peek() == Token.EndDictionary) 7 else 1
            _path!!.afterToken(endWrap)
        }
        7 -> {
            source.poll(Token.EndDictionary)
            _path!!.afterToken(Token.EndSequence)
            state = 0
        }
        else -> {
            throw AssertionError()
        }
    }

    private fun bufferName() {
        check(bufferedNameToken == null)
        bufferedNameToken = source.peek()
        bufferedName = source.poll()
    }

    private fun emitValueToken(coerceTo: Token?, nextState: Int): Any? {
        val value = source.poll(coerceTo)
        _path!!.afterToken(value)
        if (_path!!.size == pathMatcher.size + 2) {
            if (value !is Token || value.let { it == Token.EndSequence || it == Token.EndDictionary }) {
                state = nextState
            }
        }
        return value
    }

    private fun skipValueToken(nextState: Int) {
        val skipping = source.peek()
        if (skipping == Token.EndDictionary || skipping == Token.EndSequence) _path!!.afterToken(skipping)
        else _path!!.skip()

        source.skipValue()
        if (_path!!.size == pathMatcher.size + 2) {
            state = nextState
        }
    }

    override fun copyPath(): NameTracingTokenPath = // pop BeginSequence, we're representing sequences as dictionaries
        super.copyPath().also {
            check(it.removeAt(it.lastIndex) !is Index) // don't mind expectingName state, we don't use its outer part
        }

}

internal abstract class AssociateEntries(
        source: TokenStream,
        pathMatcher: Array<Predicate>,
        @JvmField protected val nameKey: Any?,
        @JvmField protected val valueKey: Any?
) : Transform(source, pathMatcher) {

    @JvmField protected val beginWrap: Token
    @JvmField protected val endWrap: Token
    @JvmField protected val nameFirstInTuple: Boolean?
    init {
        if (nameKey is Index) {
            check(valueKey is Index)
            nameFirstInTuple = if (nameKey.value == 0) {
                check(valueKey.value == 1)
                true
            } else {
                check(valueKey.value == 0)
                check(nameKey.value == 1)
                false
            }
            beginWrap = Token.BeginSequence
            endWrap = Token.EndSequence
        } else {
            check(valueKey !is Index)
            check(nameKey != valueKey)
            check(nameKey !is Token || nameKey !in Token.ControlTokens)
            check(valueKey !is Token || valueKey !in Token.ControlTokens)
            nameFirstInTuple = null
            beginWrap = Token.BeginDictionary
            endWrap = Token.EndDictionary
        }
    }

}
