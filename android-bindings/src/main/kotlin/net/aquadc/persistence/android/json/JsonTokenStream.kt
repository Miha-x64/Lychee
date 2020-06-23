@file:Suppress("NOTHING_TO_INLINE")
package net.aquadc.persistence.android.json

import android.util.Base64
import android.util.JsonReader
import android.util.JsonToken
import android.util.JsonWriter
import net.aquadc.collections.contains
import net.aquadc.collections.enumMapOf
import net.aquadc.collections.get
import net.aquadc.persistence.hasFraction
import net.aquadc.persistence.tokens.Token
import net.aquadc.persistence.tokens.TokenPath
import net.aquadc.persistence.tokens.TokenStream
import java.io.Reader
import java.io.Writer


@PublishedApi internal class JsonTokenStream(
        private val reader: JsonReader
) : TokenStream {

    private var nextNumber: String? = null
    private var nextLong: Long = Long.MIN_VALUE

    @Suppress("INAPPLICABLE_JVM_FIELD")
    private companion object {
        // wannabe const but https://youtrack.jetbrains.com/issue/KT-25915
        @JvmField val jsonToTok = enumMapOf(
                JsonToken.BEGIN_ARRAY, Token.BeginSequence,
                JsonToken.END_ARRAY, Token.EndSequence,
                JsonToken.BEGIN_OBJECT, Token.BeginDictionary,
                JsonToken.END_OBJECT, Token.EndDictionary,
                JsonToken.NAME, Token.Str,
                JsonToken.STRING, Token.Str,
                // NUMBER
                JsonToken.BOOLEAN, Token.Bool,
                JsonToken.NULL, Token.Null
                // END_DOCUMENT
        )
        @JvmField val tokToJson = enumMapOf(
                Token.Null, JsonToken.NULL,
                Token.Bool, JsonToken.BOOLEAN,
                // Token.I32, Token.I64, Token.F32, Token.F64, Token.Str, Token.Blob,
                Token.BeginSequence, JsonToken.BEGIN_ARRAY,
                Token.EndSequence, JsonToken.END_ARRAY,
                Token.BeginDictionary, JsonToken.BEGIN_OBJECT,
                Token.EndDictionary, JsonToken.END_OBJECT
        )
    }

    private val _path = TokenPath()
    override val path: List<Any?>
        get() = _path

    override fun peek(): Token {
        peekedNumber()?.let { return it }
        val token = reader.peek()
        if (nextEndArrayIsEndObject && token == JsonToken.END_ARRAY) return Token.EndDictionary
        jsonToTok[token]?.let { return it }
        if (token == JsonToken.NUMBER) {
            nextNumber = reader.nextString()
            return peekedNumber()!!
        } else if (token == JsonToken.END_DOCUMENT) {
            throw NoSuchElementException()
        } else {
            throw AssertionError()
        }
    }

    private var nextEndArrayIsEndObject = false
    override fun poll(coerceTo: Token?): Any? {
        pollNumber(coerceTo)?.let { _path.afterValue(); return it }

        val nextToken = reader.peek()!!

        // treat [] as {} if you were unlucky to deal with PHP server-side
        if (coerceTo == Token.BeginDictionary && nextToken == JsonToken.BEGIN_ARRAY) {
            reader.beginArray()
            check(!reader.hasNext()) { // fail on nonempty array the same way it would happen without this workaround
                "expected '${Token.BeginDictionary}' but was '${Token.BeginSequence}' at $path"
            }
            check(reader.isLenient) {
                "expected '${Token.BeginDictionary}' but was '${Token.BeginSequence}' at $path. " +
                        "You can either set `reader.lenient=true` to interpret empty arrays [] as empty objects {} " +
                        "or buy your server-side developer a Java/Kotlin/Clojure/Rust book"
            }
            nextEndArrayIsEndObject = true
            _path.beginObject()
            return Token.BeginDictionary
        } else if (nextEndArrayIsEndObject && nextToken == JsonToken.END_ARRAY) {
            nextEndArrayIsEndObject = false
            _path.endObject()
            return Token.EndDictionary
        }

        val nextTok = if (coerceTo == null) nextToken else {
            if (nextToken == JsonToken.NAME && coerceTo != Token.Str)
                // JsonReader can't coerce names to other types, let's do it ourselves
                return coerceTo.coerce(reader.nextName().also(_path::onName))

            tokToJson[coerceTo]
        }

        return if (nextTok == null) {
            val value = when (coerceTo!!) {
                Token.I32 -> reader.nextInt()
                Token.I64 -> reader.nextLong()
                Token.F32 -> reader.nextDouble().toFloat()
                Token.F64 -> reader.nextDouble()
                Token.Str ->
                    if (nextToken == JsonToken.NAME) reader.nextName().also(_path::onName)
                    else reader.nextString().also { _path.afterValue() }
                Token.Blob -> {
                    val isName = nextToken == JsonToken.NAME
                    val value = Base64.decode(
                            if (isName) reader.nextName().also(_path::onName)
                            else reader.nextString()
                  , Base64.DEFAULT)
                    if (isName) _path.onName(value) else _path.afterValue()
                    value
                }
                else -> throw AssertionError() // already handled by map
            }
            if (coerceTo in Token.Numbers) _path.afterValue()
            value
        } else when (nextTok) {
            JsonToken.BEGIN_ARRAY -> {
                reader.beginArray()
                _path.beginArray()
                Token.BeginSequence
            }
            JsonToken.END_ARRAY -> {
                reader.endArray()
                _path.endArray()
                Token.EndSequence
            }
            JsonToken.BEGIN_OBJECT -> {
                reader.beginObject()
                _path.beginObject()
                Token.BeginDictionary
            }
            JsonToken.END_OBJECT -> {
                reader.endObject()
                _path.endObject()
                Token.EndDictionary
            }
            JsonToken.NAME -> reader.nextName().also(_path::onName)
            JsonToken.STRING -> reader.nextString().also { _path.afterValue() }
            JsonToken.BOOLEAN -> reader.nextBoolean().also { _path.afterValue() }
            JsonToken.NULL -> reader.nextNull().let { _path.afterValue(); null }
            JsonToken.NUMBER -> {
                nextNumber = reader.nextString()
                val n = pollNumber(null)!!
                _path.afterValue()
                n
            }
            JsonToken.END_DOCUMENT -> throw NoSuchElementException()
        }
    }

    override fun skipValue() {
        if (nextNumber != null) {
            nextNumber = null
            nextLong = Long.MIN_VALUE
            _path.afterValue()
        } else when (reader.peek()) {
            // nope, I'm not gonna call skipValue here: https://github.com/google/gson/issues/605
            JsonToken.END_ARRAY -> {
                reader.endArray()
                _path.endArray()
            }
            JsonToken.END_OBJECT -> {
                reader.endObject()
                _path.endObject()
            }
            else -> {
                reader.skipValue()
                _path.afterValue()
            }
        }
    }

    override fun hasNext(): Boolean =
            nextNumber != null || reader.peek() != JsonToken.END_DOCUMENT

    // finds out the correct type for the given stringified number
    private fun peekedNumber(): Token? = nextNumber?.let { nextNumber ->
        if (hasFrac(nextNumber)) Token.F64
        else {
            if (nextLong == Long.MIN_VALUE) nextLong = nextNumber.toLong()
            when (nextLong) {
                in Int.MIN_VALUE..Int.MAX_VALUE -> Token.I32
                else -> Token.I64
            }
        }
    }

    private fun pollNumber(coerceTo: Token?): Any? {
        val nextNumber = this.nextNumber ?: return null
        this.nextNumber = null

        if (coerceTo == Token.Str) return nextNumber
        if (hasFrac(nextNumber)) return when (coerceTo) {
            Token.F32 -> nextNumber.toFloat()
            null, Token.F64 -> nextNumber.toDouble()
            else -> throw IllegalArgumentException("expected '$coerceTo' but was '${Token.F64}' at $path")
        }
        val nextLong = if (this.nextLong == Long.MIN_VALUE) nextNumber.toLong() else this.nextLong
        this.nextLong = Long.MIN_VALUE

        if (coerceTo == null) {
            return if (nextLong in Int.MIN_VALUE..Int.MAX_VALUE) nextLong.toInt() else nextLong
        }
        try {
            return coerceTo.coerceToNumber(nextLong)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("${e.message} at $path")
        }
    }

    private fun hasFrac(number: String): Boolean = try {
        hasFraction(number)
    } catch (e: NumberFormatException) {
        throw NumberFormatException("${e.message} at $path")
    }

    override fun close() {
        reader.close()
    }

}
