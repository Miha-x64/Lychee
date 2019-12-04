package net.aquadc.persistence.extended.tokens

import net.aquadc.persistence.tokens.Token
import net.aquadc.persistence.tokens.TokenStream
import net.aquadc.persistence.tokens.coerce


@PublishedApi internal class InlineTokens(
        source: TokenStream,
        pathMatcher: Array<Predicate>,
        private val isVictim: Predicate,
        private val rename: (Any?) -> Any?,
        private val merge: (target: MutableMap<Any?, Any?>, key: Any?, value: Any?) -> Unit,
        private val buffer: MutableMap<Any?, Any?>
) : Transform(source, pathMatcher) {

    private var tokensToInline: ArrayList<Any?>? = null
    private var inlinedMappings: Iterator<Map.Entry<Any?, Any?>>? = null
    private var inlinedMapping: Map.Entry<Any?, Any?>? = null
    private var inlineIndex = -1

    override fun peek(): Token =
            inlinedMapping?.let { entry ->
                val value =
                        if (inlineIndex == -1) entry.key
                        else entry.value.let { if (it is List<*>) it[inlineIndex] else it }
                Token.ofValue(value) ?: throw IllegalArgumentException("bad value: $value")
            } ?: source.peek()

    override fun poll(coerceTo: Token?): Any? = inlinedMapping?.let { entry ->
        return coerceTo.coerce(if (inlineIndex == -1) {
            inlineIndex = 0
            entry.key.also(_path!!::onName)
        } else {
            var nextMapping = true
            val v = entry.value.let {
                if (it is MutableList<*>) {
                    @Suppress("UNCHECKED_CAST")
                    it as MutableList<Any?>
                    val v = it.set(inlineIndex++, null)
                    if (inlineIndex < it.size) {
                        nextMapping = false
                    }
                    v
                } else {
                    it
                }
            }
            if (nextMapping) {
                inlineIndex = -1
                inlinedMappings?.let {
                    inlinedMapping = it.next()
                    if (!it.hasNext()) inlinedMappings = null
                } ?: run {
                    inlinedMapping = null
                    // the end of inlined map!
                    buffer.clear()
                }
            }
            v.also(_path!!::afterToken)
        })
    } ?: run {
        val v = source.poll(coerceTo)

        _path = null
        // null out path on source.poll:
        // after giving away all inlined information, and before exiting EndDictionary,
        // source may remember name of object we've inlined, so don't show it

        // plusNesting=1 means we're within some object/sequence and we have either name or index
        if (v == Token.BeginDictionary && matches(1)) {
            check(buffer.isEmpty())
            copyPath()
            check(inlinedMapping == null)
            check(inlinedMappings == null)
            check(inlineIndex == -1)
            val tokensToInline = tokensToInline?.also { check(it.isEmpty()) }
                    ?: ArrayList<Any?>().also { tokensToInline = it }
            // let's gather dictionary contents
            while (source.peek() != Token.EndDictionary) {
                val name = source.poll().checkName()
                if (isVictim(name)) {
                    // gather tokens to inline them later
                    source.poll(Token.BeginDictionary)
                    while (true) {
                        val inlineName = source.poll()
                        if (inlineName is Token) {
                            check(inlineName == Token.EndDictionary)
                            break
                        }
                        tokensToInline.add(rename(inlineName.checkName()))
                        tokensToInline.add(source.pollValue())
                    }
                } else {
                    // this assertion ignores possible null keys collisions, but let it be
                    check(buffer.put(name, source.pollValue()) == null)
                }
            }

            // okay, now we've got a content Map and pairs of tokens to inline...
            for (i in 0 until tokensToInline.size step 2) {
                merge(buffer, tokensToInline[i], tokensToInline[i + 1])
            }

            tokensToInline.clear()

            if (buffer.isNotEmpty()) {
                val itr = buffer.iterator()
                inlinedMapping = itr.next()
                if (itr.hasNext()) inlinedMappings = itr
            }
        }
        v
    }

    override fun skipValue(): Unit =
            if (inlinedMapping == null) {
                _path = null
                source.skipValue()
            } else super.skipValue() // just poll next value, it is already parsed & in memory

}

@PublishedApi internal class OutlineTokens(
        source: TokenStream,
        pathMatcher: Array<Predicate>,
        private val what: Predicate,
        private val newName: Any,
        private val rename: (Any?) -> Any?
) : Transform(source, pathMatcher) {

    private var outlining = -3

    private var nextNameToken: Token? = null
    private var nextName: Any? = null
    private var expectingName = true
    private var buffer = ArrayList<Any?>()

    override fun peek(): Token =
            when (outlining) {
                // not outlining, just hanging around, you know...
                -3 -> source.peek()

                // inside object of interest! Gathering some tokens into a buffer
                -2 -> {
                    if (expectingName && source.path.size == pathMatcher.size + 1) {
                        nextNameToken
                                ?: advance().let { nextNameToken }
                                ?: (Token.ofValue(newName) ?: throw IllegalArgumentException("bad value: $newName"))
                                        .also { check(expectingName); outlining = -1 }
                    } else {
                        source.peek()
                    }
                }

                // gonna emit name of outlined object, this separate step is importanta for path
                -1 -> {
                    Token.ofValue(newName) ?: throw IllegalArgumentException("bad value: $newName")
                }

                // outlining, i. e. emitting from buffer
                else -> {
                    Token.ofValue(buffer[outlining]) ?: throw IllegalArgumentException("bad value: ${buffer.first()}")
                }
            }

    override fun poll(coerceTo: Token?): Any? = when (outlining) {
        -3 -> {
            val value = source.poll(coerceTo)
            if (value == Token.BeginDictionary && matches(1)) {
                outlining = -2
                expectingName = true
            } else {
                _path = null
            }
            value
        }
        -2 -> {
            val expectingName = expectingName
            val noAdditionalNesting = source.path.size == pathMatcher.size + 1
            if (noAdditionalNesting) this.expectingName = !expectingName
            if (expectingName && noAdditionalNesting) {
                if (nextNameToken == null) advance()
                nextNameToken?.let {
                    val nn = nextName
                    nextNameToken = null
                    nextName = null
                    return coerceTo.coerce(nn)
                }
                // else EndDictionary reached, outlining now
                startOutlining()
                coerceTo.coerce(newName)
            } else source.poll(coerceTo)
        }
        -1 -> {
            startOutlining()
            coerceTo.coerce(newName)
        }
        else -> {
            val value = buffer.set(outlining++, null)
            if (buffer.size == outlining) {
                buffer.clear()
                outlining = -3
            }
            _path!!.afterToken(value)
            coerceTo.coerce(value)
        }
    }

    private fun startOutlining() {
        outlining = 0
        copyPath().also {
            it.expectingName.add(true)
            it.afterToken(newName)
        }
    }

    private fun advance() {
        if (buffer.isEmpty()) {
            buffer.add(Token.BeginDictionary)
        }

        while (true) {
            val tok = source.peek()

            if (tok == Token.EndDictionary) {
                buffer.add(Token.EndDictionary)
                return
            }

            val name = source.poll().checkName()
            if (what(name)) {
                // bufferize both key and value
                buffer.add(rename(name))
                source.writeBracketSequenceTo(buffer, source.poll())
                // and go fetch next...
            } else {
                // we're not interested in outlining these, let's show them to consumer
                nextNameToken = tok
                nextName = name
                return
            }
        }
    }


    override fun skipValue(): Unit = when (outlining) {
        -3 -> {
            source.skipValue()
            _path = null
        }
        -2 -> {
            val expectingName = expectingName
            val noAdditionalNesting = source.path.size == pathMatcher.size + 1
            if (noAdditionalNesting) this.expectingName = !expectingName
            if (expectingName && noAdditionalNesting) {
                if (nextNameToken == null) advance()
                nextNameToken?.let {
                    nextNameToken = null
                    nextName = null
                    return
                }
                // else source EndDictionary reached, outlining now
                startOutlining()
            } else source.skipValue()
        }
        -1 -> {
            startOutlining() // they've skipped our nested name
        }
        0 -> { // they've skipped BeginDictionary, drop the whole buffer
            buffer.clear()
            outlining = -3
        }
        else -> {
            super.skipValue() // within buffer, just traverse in-memory objects
        }
    }

}
