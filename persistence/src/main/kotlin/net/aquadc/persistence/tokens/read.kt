package net.aquadc.persistence.tokens

import net.aquadc.collections.enumMapOf
import net.aquadc.collections.get
import net.aquadc.collections.set
import net.aquadc.persistence.readPartial
import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.type.DataType


/**
 * Read these tokens as [type].
 * This asserts the next token is [Token.BeginDictionary] and consumes the whole bracket sequence.
 */
@Suppress("UNCHECKED_CAST")
fun <T> TokenStream.readAs(type: DataType<T>): T {
    val type = if (type is DataType.Nullable<*, *>) {
        if (peek() == Token.Null) return poll(Token.Null) as T
        type.actualType
    } else type

    return when (type) {
        is DataType.Nullable<*, *> -> throw AssertionError()
        is DataType.Simple -> {
            val token = kindToToken[type.kind]!! // !! exhaustive mapping
            type.load(poll(token)!!) as T // !! we never pass Token.Null so source must not return `null`s
        }
        is DataType.Collect<*, *, *> -> {
            type.load(readListOf(type.elementType)) as T
        }
        is DataType.Partial<*, *> -> {
            poll(Token.BeginDictionary)
            val byName = type.schema.fieldsByName
            @Suppress("UPPER_BOUND_VIOLATED")
            val struct = readPartial<Any?, Schema<*>>(
                    type as DataType.Partial<Any?, Schema<*>>, fieldValues,
                    { nextField(byName) as FieldDef<Schema<*>, *, *>? }, { readAs(it) }
            )
            poll(Token.EndDictionary)
            struct as T
        }
    }
}

private fun TokenStream.nextField(byName: Map<String, FieldDef<*, *, *>>): FieldDef<*, *, *>? {
    while (peek() != Token.EndDictionary) {
        val field = byName[poll(Token.Str) as String]
        if (field == null) skipValue() // unsupported value
        else return field
    }
    return null
}

private val fieldValues = ThreadLocal<ArrayList<Any?>>()
/**
 * Read these tokens as a sequence of [type].
 * This asserts the next token is [Token.BeginSequence] and consumes the whole bracket sequence.
 * @see readAs
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

@JvmSynthetic internal val kindToToken = enumMapOf(
        DataType.Simple.Kind.Bool, Token.Bool,
        DataType.Simple.Kind.I8, Token.I8,
        DataType.Simple.Kind.I16, Token.I16,
        DataType.Simple.Kind.I32, Token.I32,
        DataType.Simple.Kind.I64, Token.I64,
        DataType.Simple.Kind.F32, Token.F32,
        DataType.Simple.Kind.F64, Token.F64,
        DataType.Simple.Kind.Str, Token.Str
).also {
    it[DataType.Simple.Kind.Blob] = Token.Blob // enumMapOf has max. 8 key-value pairs
}
