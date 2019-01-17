package net.aquadc.properties.android.persistence

import android.util.Base64
import android.util.JsonReader
import android.util.JsonToken
import android.util.JsonWriter
import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.FieldSet
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.struct.StructBuilder
import net.aquadc.persistence.struct.StructSnapshot
import net.aquadc.persistence.struct.allFieldSet
import net.aquadc.persistence.struct.build
import net.aquadc.persistence.struct.forEach
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.match


/**
 * Reads a JSON 'array' of 'objects' as a list of [StructSnapshot]s according to [SCH],
 * consuming both opening and closing square braces.
 * Each object is read using [read].
 */
fun <SCH : Schema<SCH>> JsonReader.readListOf(schema: SCH): List<StructSnapshot<SCH>> {
    beginArray()
    val list = if (!hasNext()) emptyList() else {
        val first = read(schema)

        if (!hasNext()) listOf(first) else {
            val list = ArrayList<StructSnapshot<SCH>>()
            list.add(first)

            do list.add(read(schema))
            while (hasNext())

            list
        }
    }
    endArray()

    return list
}

/**
 * Reads a JSON 'object' as a single [StructSnapshot] according to [SCH],
 * ignoring key-value pairs not listed in [SCH],
 * consuming both opening and closing curly braces.
 * Throws an exception if there was no value for any [FieldDef] without a default value,
 * or if [JsonReader] meets unexpected token for the given [FieldDef.type].
 */
fun <SCH : Schema<SCH>> JsonReader.read(schema: SCH): StructSnapshot<SCH> = schema.build { builder ->
    beginObject()
    while (hasNext()) {
        val name = nextName()
        val field = fieldsByName[name]

        if (field === null) skipValue()
        else readValueInto(builder, field)
    }
    endObject()
}

private fun <SCH : Schema<SCH>, T> JsonReader.readValueInto(target: StructBuilder<SCH>, field: FieldDef<SCH, T>) {
    val type = field.type

    target[field] = type.match { isNullable, simple ->
        if (isNullable && peek() === JsonToken.NULL) {
            null as T
        } else {
            type.decode(when (simple.kind) {
                DataType.Simple.Kind.Bool -> nextBoolean()
                DataType.Simple.Kind.I8 -> nextInt().assertFitsByte()
                DataType.Simple.Kind.I16 -> nextInt().assertFitsShort()
                DataType.Simple.Kind.I32 -> nextInt()
                DataType.Simple.Kind.I64 -> nextLong()
                DataType.Simple.Kind.F32 -> nextDouble().toFloat()
                DataType.Simple.Kind.F64 -> nextDouble()
                DataType.Simple.Kind.Str -> nextString()
                DataType.Simple.Kind.Blob -> Base64.decode(nextString(), Base64.DEFAULT)
            })
        }
    }
}

/**
 * Writes a list of [Struct]s as a JSON 'array' of 'objects',
 * including both opening and closing square braces.
 * Each object is written using [write] ([Struct]) overload`.
 */
fun <SCH : Schema<SCH>> JsonWriter.write(
        list: List<Struct<SCH>>,
        fields: FieldSet<SCH, FieldDef<SCH, *>> =
                list.firstOrNull()?.schema?.allFieldSet() ?: /* otherwise it doesn't matter */ FieldSet(0)
) {
    beginArray()
    list.forEach { write(it, fields) }
    endArray()
}

/**
 * Writes a single [Struct] instance according to [SCH],
 * including [FieldDef]s where value is equal to the default one,
 * writing both opening and closing curly braces.
 */
fun <SCH : Schema<SCH>> JsonWriter.write(
        struct: Struct<SCH>,
        fields: FieldSet<SCH, FieldDef<SCH, *>> =
                struct.schema.allFieldSet()
) {
    beginObject()
    struct.schema.forEach(fields) { field ->
        name(field.name)
        writeValueFrom(struct, field)
    }
    endObject()
}

@Suppress("IMPLICIT_CAST_TO_ANY")
private fun <SCH : Schema<SCH>, T> JsonWriter.writeValueFrom(struct: Struct<SCH>, field: FieldDef<SCH, T>) {
    val type = field.type
    val value = struct[field]

    type.match { isNullable, simple ->
        if (isNullable && value === null) {
            nullValue()
        } else {
            val raw = type.encode(value)
            when (simple.kind) {
                DataType.Simple.Kind.Bool -> value(raw as Boolean)
                DataType.Simple.Kind.I8 -> value((raw as Byte).toInt())
                DataType.Simple.Kind.I16 -> value((raw as Short).toInt())
                DataType.Simple.Kind.I32 -> value(raw as Int)
                DataType.Simple.Kind.I64 -> value(raw as Double)
                DataType.Simple.Kind.F32 -> value(raw as Float)
                DataType.Simple.Kind.F64 -> value(raw as Double)
                DataType.Simple.Kind.Str -> value(raw as String)
                DataType.Simple.Kind.Blob -> Base64.encode(raw as ByteArray, Base64.DEFAULT)
            }.also { }
        }
    }
}
