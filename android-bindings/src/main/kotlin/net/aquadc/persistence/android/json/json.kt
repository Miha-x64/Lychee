@file:[
    JvmName("Json")
    Suppress("NOTHING_TO_INLINE")
]
package net.aquadc.persistence.android.json

import android.util.Base64
import android.util.JsonReader
import android.util.JsonWriter
import net.aquadc.persistence.each
import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.FieldSet
import net.aquadc.persistence.struct.PartialStruct
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.struct.forEach
import net.aquadc.persistence.tokens.Token
import net.aquadc.persistence.tokens.TokenStream
import net.aquadc.persistence.tokens.readAs
import net.aquadc.persistence.tokens.tokensFrom
import net.aquadc.persistence.type.DataType
import java.io.Reader
import java.io.Writer


/**
 * Writes a list of [Struct]s as a JSON 'array' of 'objects',
 * including both opening and closing square braces.
 * Each object is written using [write] ([Struct], [FieldSet]) overload`.
 */
// TODO: filter stream instead
fun <SCH : Schema<SCH>> JsonWriter.write(
        list: List<Struct<SCH>>,
        fields: FieldSet<SCH, FieldDef<SCH, *, *>> =
                list.firstOrNull()?.schema?.allFieldSet ?: /* otherwise it doesn't matter */ FieldSet(0)
) {
    beginArray()
    list.each { write(it, fields) }
    endArray()
}

/**
 * Writes a single [Struct] instance according to [SCH],
 * including [FieldDef]s where value is equal to the default one,
 * writing both opening and closing curly braces.
 */
// TODO: filter stream instead
fun <SCH : Schema<SCH>> JsonWriter.write(
        struct: Struct<SCH>,
        fields: FieldSet<SCH, FieldDef<SCH, *, *>>
) {
    beginObject()
    struct.schema.run {
        forEach(fields) { field ->
            name(field.name.toString())
            writeValueFrom(struct, field)
        }
    }
    endObject()
}

@Suppress("NOTHING_TO_INLINE") // just capture T and assert value is present
private inline fun <SCH : Schema<SCH>, T> JsonWriter.writeValueFrom(struct: PartialStruct<SCH>, field: FieldDef<SCH, T, *>) =
        struct.schema.run { (field as FieldDef<SCH, T, DataType<T>>).type }
            .tokensFrom(struct.getOrThrow(field)).writeTo(this)

// STREAMS

/**
 * Create a [JsonReader] for this [Reader].
 * For example,
 * <code>
 *     inputStream.reader().json().tokens().iteratorOfTransient(SomeSchema).use {
 *         forEachRemaining {
 *             println(it)
 *         }
 *     }
 * </code>
 */
inline fun Reader.json(): JsonReader =
    JsonReader(this)

/**
 * Create a [JsonWriter] for this [Writer].
 */
inline fun Writer.json(): JsonWriter =
    JsonWriter(this)

/**
 * Create a [TokenStream] of this JSON.
 */
inline fun JsonReader.tokens(): TokenStream =
    JsonTokenStream(this)

/**
 * Write [this] [TokenStream] to [writer].
 */
fun TokenStream.writeTo(writer: JsonWriter): Unit =
    writeBracketSequenceTo(writer, poll())

private fun TokenStream.writeBracketSequenceTo(writer: JsonWriter, token: Any?) {
    when (token) {
        null -> writer.nullValue()
        is Boolean -> writer.value(token)
        is Byte, is Short, is Int, is Long -> writer.value((token as Number).toLong())
        is Float, is Double -> writer.value((token as Number).toDouble())
        is CharSequence -> writer.value(token.toString())
        is ByteArray -> writer.value(Base64.encodeToString(token, Base64.DEFAULT))
        Token.BeginSequence -> {
            writer.beginArray()
            while (true) {
                val next = poll()
                if (next == Token.EndSequence) break
                writeBracketSequenceTo(writer, next)
            }
            writer.endArray()
        }
        Token.BeginDictionary -> {
            writer.beginObject()
            while (true) {
                if (peek() == Token.EndDictionary) {
                    poll(Token.EndDictionary)
                    writer.endObject()
                    break
                }
                writer.name((poll(Token.Str) as CharSequence).toString())
                writeBracketSequenceTo(writer, poll())
            }
        }
        Token.EndSequence, Token.EndDictionary -> {
            throw IllegalArgumentException("unexpected token '$token', nesting problem at $path")
        }
        else -> throw AssertionError("unexpected token '$token'")
    }
}
