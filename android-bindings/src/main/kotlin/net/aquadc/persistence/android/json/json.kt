@file:JvmName("Json")
package net.aquadc.persistence.android.json

import android.util.JsonReader
import android.util.JsonWriter
import net.aquadc.persistence.each
import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.FieldSet
import net.aquadc.persistence.struct.PartialStruct
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.struct.allFieldSet
import net.aquadc.persistence.struct.approxType
import net.aquadc.persistence.struct.forEach
import net.aquadc.persistence.tokens.readAs
import net.aquadc.persistence.tokens.readListOf
import net.aquadc.persistence.tokens.tokens
import net.aquadc.persistence.tokens.tokensFrom
import net.aquadc.persistence.type.DataType


/**
 * Reads a JSON 'array' of values denoted by [type] as a list of [T]s,
 * consuming both opening and closing square braces.
 * Each value is read using [readAs].
 */
@Deprecated("use tokens() directly instead", ReplaceWith("this.tokens().readListOf(type)",
        "net.aquadc.persistence.android.json.tokens", "net.aquadc.persistence.tokens.readListOf"))
fun <T> JsonReader.readListOf(type: DataType<T>): List<T> =
        tokens().readListOf(type)

/**
 * Reads a JSON value denoted by [type] as [T].
 *
 * For [Schema]s and [DataType.Partial]s,
 * this ignores key-value pairs not listed in schema,
 * and consumes both opening and closing curly braces.
 * Throws an exception if there was no value for any [FieldDef] without a default value,
 * or if [JsonReader] met unexpected token for the given [FieldDef.type].
 */
@Deprecated("use tokens() directly instead", ReplaceWith("this.tokens().readAs(type)",
        "net.aquadc.persistence.android.json.tokens", "net.aquadc.persistence.tokens.readAs"))
fun <T> JsonReader.read(type: DataType<T>): T =
        tokens().readAs(type)


/**
 * Writes a list of [Struct]s as a JSON 'array' of 'objects',
 * including both opening and closing square braces.
 * Each object is written using [write] ([Struct], [FieldSet]) overload`.
 */
// TODO: filter stream instead
fun <SCH : Schema<SCH>> JsonWriter.write(
        list: List<Struct<SCH>>,
        fields: FieldSet<SCH, FieldDef<SCH, *, *>> =
                list.firstOrNull()?.schema?.allFieldSet() ?: /* otherwise it doesn't matter */ FieldSet(0)
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
    struct.schema.forEach(fields) { field ->
        name(field.name)
        writeValueFrom(struct, field)
    }
    endObject()
}

@Deprecated("use tokens() directly instead", ReplaceWith("struct.tokens().writeTo(this)",
        "net.aquadc.persistence.tokens.tokens", "net.aquadc.persistence.android.json.writeTo"))
fun <SCH : Schema<SCH>> JsonWriter.write(struct: Struct<SCH>): Unit =
        struct.tokens().writeTo(this)

/**
 * Writes a value denoted by [type].
 */
@Deprecated("use tokens() directly instead", ReplaceWith("type.tokensFrom(value).writeTo(this)",
        "net.aquadc.persistence.tokens.tokensFrom", "net.aquadc.persistence.android.json.writeTo"))
fun <T> JsonWriter.write(type: DataType<T>, value: T): Unit =
        type.tokensFrom(value).writeTo(this)


@Suppress("NOTHING_TO_INLINE") // just capture T and assert value is present
private inline fun <SCH : Schema<SCH>, T> JsonWriter.writeValueFrom(struct: PartialStruct<SCH>, field: FieldDef<SCH, T, *>) =
        write(field.approxType, struct.getOrThrow(field))
