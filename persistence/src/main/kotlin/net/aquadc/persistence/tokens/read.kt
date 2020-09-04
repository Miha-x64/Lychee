@file:JvmName("ReadTokens")
package net.aquadc.persistence.tokens

import android.util.JsonReader
import net.aquadc.collections.enumMapOf
import net.aquadc.collections.get
import net.aquadc.collections.set
import net.aquadc.persistence.CloseableIterator
import net.aquadc.persistence.IteratorAndTransientStruct
import net.aquadc.persistence.NullSchema
import net.aquadc.persistence.forceAddField
import net.aquadc.persistence.readPartial
import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.FieldSet
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.struct.ordinal
import net.aquadc.persistence.struct.size
import net.aquadc.persistence.struct.toString
import net.aquadc.persistence.type.DataType


/**
 * Read these tokens as [type].
 *
 * For [Schema]s and [DataType.NotNull.Partial]s,
 * this ignores key-value pairs not listed in schema,
 * and consumes both opening and closing curly braces.
 * Throws an exception if there was no value for any [FieldDef] without a default value,
 * or if [JsonReader] met unexpected token for the given [Schema.type].
 */
@Suppress("UNCHECKED_CAST")
fun <T> TokenStream.readAs(type: DataType<T>): T {
    val type = if (type is DataType.Nullable<*, *>) {
        if (peek() == Token.Null) return poll(Token.Null) as T
        type.actualType
    } else type

    return when (type) {
        is DataType.Nullable<*, *> -> throw AssertionError()
        is DataType.NotNull.Simple -> {
            val token =
                if (type.hasStringRepresentation) Token.Str
                else kindToToken[type.kind]!! // !! exhaustive mapping
            type.load(poll(token)!!) as T // !! we never pass Token.Null so source must not return `null`s
        }
        is DataType.NotNull.Collect<*, *, *> -> {
            type.load(readListOf(type.elementType)) as T
        }
        is DataType.NotNull.Partial<*, *> -> {
            poll(Token.BeginDictionary)
            val sch = type.schema
            val struct = readPartial(
                type as DataType.NotNull.Partial<Any?, NullSchema>, fieldValues,
                { nextField(sch) }, { readAs(it) }
            )
            poll(Token.EndDictionary)
            struct as T
        }
    }
}

internal/*accessed from a class*/ fun TokenStream.nextField(sch: Schema<*>): Byte {
    while (peek() != Token.EndDictionary) {
        sch.fieldByName(poll(Token.Str) as CharSequence,
                { return it.ordinal },
                { skipValue() /* unsupported value */ }
        )
    }
    return -1
}

private val fieldValues = ThreadLocal<ArrayList<Any?>>()
/**
 * Collect these tokens as a sequence of [type] to a [List].
 * This asserts that next token is [Token.BeginSequence] and consumes the whole bracket sequence.
 *
 * Each value is read using [readAs].
 *
 * @see readAs
 * @see iteratorOf
 * @see iteratorOfTransient
 */
fun <T> TokenStream.readListOf(type: DataType<T>): List<T> {
    // TODO: when [type] is primitive, use specialized collections

    poll(Token.BeginSequence)
    val list = if (peek() == Token.EndSequence) emptyList() else {
        val first = readAs(type)

        if (peek() == Token.EndSequence) listOf(first) else {
            val list = ArrayList<T>()
            list.add(first)

            do list.add(readAs(type))
            while (peek() != Token.EndSequence)

            list
        }
    }
    poll(Token.EndSequence)

    return list
}

/**
 * A view on [TokenStream] as a sequence of [type].
 * This asserts that next token is [Token.BeginSequence] and consumes the whole bracket sequence.
 * @see readAs
 * @see readListOf
 * @see iteratorOfTransient
 */
@Suppress("NOTHING_TO_INLINE")
inline fun <T> TokenStream.iteratorOf(type: DataType<T>): CloseableIterator<T> =
        TokensIterator(this, type, NullSchema)

/**
 * A view on [TokenStream] as a sequence of __transient structs__ of type [schema].
 * This asserts that next token is [Token.BeginSequence]
 * and consumes the whole bracket sequence by the last `next()` call.
 * A [Struct] is __transient__ when it is owned by an [Iterator].
 * Such a [Struct] is valid only until [Iterator.next] or [CloseableIterator.close] call.
 * Never store, collect, or let them escape the for-loop.
 * Sorting, finding min, max, distinct also won't work because
 * these operations require looking back at previous [Struct]s.
 * (Flat)mapping and filtering i.e. stateless intermediate operations are still OK.
 * Limiting, skipping, folding, reducing, counting,
 * and other stateful one-pass operations are also OK.
 *
 * @see readAs
 * @see readListOf
 * @see iteratorOf
 */
@Suppress("NOTHING_TO_INLINE")
inline fun <SCH : Schema<SCH>> TokenStream.iteratorOfTransient(schema: SCH): CloseableIterator<Struct<SCH>> =
        TokensIterator(this, null, schema)
// I could create TokenStream.iterateTransient(schema: SCH, block: (Iterator<Struct<SCH>>) -> R): R but
// * you could want to read several token sequences within a single document, auto-closing will complicate things
// * you still can do e. g. `tokens.iteratorOfTransient(sch) { itr -> itr.next() }` and shoot your leg
//   (should I create a `TransientIterator.next { el -> }`?)


@Suppress("UNCHECKED_CAST") @PublishedApi
internal class TokensIterator<SCH : Schema<SCH>, T>(
    private val tokens: TokenStream,
    private val type: DataType<T>?, // transient if null
    transientSchema: SCH // stable if NullSchema
) : IteratorAndTransientStruct<SCH, T>(transientSchema) {

    private var state = 0
    private var array: Array<Any?>? = null // for transient struct

    override fun hasNext(): Boolean {
        if (state == 0) pollBegin()
        return state == 1
    }
    override fun next(): T {
        if (state == 0) pollBegin()
        if (state == 2) throw NoSuchElementException()
        return (if (type == null) readTransient() as T else tokens.readAs(type)).also { peekEnd() }
    }

    override fun <T> get(field: FieldDef<SCH, T, *>): T =
        array!![field.ordinal.toInt()] as T

    override fun close() {
        state = 2
        tokens.close()
        array = null
    }

    private fun pollBegin() {
        tokens.poll(Token.BeginSequence)
        state =
            if (tokens.peek() === Token.EndSequence) 2 // Short-circuit: empty sequence.
            else 1.also {
                if (type == null)
                    array = arrayOfNulls(schema.allFieldSet.size) // Allocate data structure for transient Struct.
            }
    }
    private fun readTransient(): Struct<SCH> {
        tokens.poll(Token.BeginDictionary)

        var fields = 0L
        val values: Array<Any?> = array!!

        var nextField = tokens.nextField(schema)
        while (nextField.toInt() != -1) { // this is ultra unhandy without assignment as expression
            fields = fields.forceAddField(nextField, schema)
            values[nextField.toInt()] = tokens.readAs(schema.typeAt(nextField))

            nextField = tokens.nextField(schema)
        }

        val missing = schema.allFieldSet.bitSet and fields.inv()
        if (missing != 0L)
            throw NoSuchElementException("Missing values for fields: ${schema.toString(FieldSet(missing))}")

        tokens.poll(Token.EndDictionary)
        return this
    }
    private fun peekEnd() {
        if (tokens.peek() == Token.EndSequence) {
            tokens.poll()
            state = 2
            // The end. But this doesn't mean we can null out array or close tokens!
        }
    }
}

@JvmSynthetic internal val kindToToken = enumMapOf(
    DataType.NotNull.Simple.Kind.Bool, Token.Bool,
    DataType.NotNull.Simple.Kind.I32, Token.I32,
    DataType.NotNull.Simple.Kind.I64, Token.I64,
    DataType.NotNull.Simple.Kind.F32, Token.F32,
    DataType.NotNull.Simple.Kind.F64, Token.F64,
    DataType.NotNull.Simple.Kind.Str, Token.Str
).also {
    it[DataType.NotNull.Simple.Kind.Blob] = Token.Blob // enumMapOf has max. 8 key-value pairs
}
