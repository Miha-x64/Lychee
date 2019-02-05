@file:JvmName("Json")
package net.aquadc.properties.android.persistence.json

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
import net.aquadc.persistence.type.DataTypeVisitor
import net.aquadc.persistence.type.match
import net.aquadc.properties.android.persistence.assertFitsByte
import net.aquadc.properties.android.persistence.assertFitsShort


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

// duplicate will go away when Schema will become a DataType
private fun JsonReader.readEncodedList(type: DataType<*>): List<Any?> {
    beginArray()
    val list = if (!hasNext()) emptyList() else {
        val first = readEncodedValue(type)

        if (!hasNext()) listOf(first) else {
            val list = ArrayList<Any?>()
            list.add(first)

            do list.add(readEncodedValue(type))
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

@Suppress("NOTHING_TO_INLINE") // exists just to capture type into T
private inline fun <SCH : Schema<SCH>, T> JsonReader.readValueInto(target: StructBuilder<SCH>, field: FieldDef<SCH, T>) {
    target[field] = field.type.decode(readEncodedValue(field.type))
}

private val readerVis = JsonReaderVisitor<Any?>()
private fun <T> JsonReader.readEncodedValue(type: DataType<T>): Any? =
        (readerVis as JsonReaderVisitor<T>).match(type, this, null)
private class JsonReaderVisitor<T> : DataTypeVisitor<JsonReader, Nothing?, T, Any?> {

    override fun JsonReader.simple(arg: Nothing?, raw: DataType<T>, kind: DataType.Simple.Kind): Any? =
            if (raw is DataType.Nullable<*> && peek() === JsonToken.NULL) {
                skipValue()
                null
            } else when (kind) {
                DataType.Simple.Kind.Bool -> nextBoolean()
                DataType.Simple.Kind.I8 -> nextInt().assertFitsByte()
                DataType.Simple.Kind.I16 -> nextInt().assertFitsShort()
                DataType.Simple.Kind.I32 -> nextInt()
                DataType.Simple.Kind.I64 -> nextLong()
                DataType.Simple.Kind.F32 -> nextDouble().toFloat()
                DataType.Simple.Kind.F64 -> nextDouble()
                DataType.Simple.Kind.Str -> nextString()
                DataType.Simple.Kind.Blob -> Base64.decode(nextString(), Base64.DEFAULT)
            }

    override fun <E> JsonReader.collection(arg: Nothing?, raw: DataType<T>, type: DataType.Collect<T, E>): Any? =
            if (raw is DataType.Nullable<*> && peek() === JsonToken.NULL) {
                skipValue()
                null
            } else readEncodedList(type.elementType)
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

@Suppress("NOTHING_TO_INLINE") // just capture T
private inline fun <SCH : Schema<SCH>, T> JsonWriter.writeValueFrom(struct: Struct<SCH>, field: FieldDef<SCH, T>) =
        writeEncoded(field.type, field.type.encode(struct[field]))

private val writerVis = JsonWriterVisitor<Any?>()
private fun <T> JsonWriter.writeEncoded(type: DataType<T>, value: Any?) =
        (writerVis as JsonWriterVisitor<T>).match(type, this, value)

private class JsonWriterVisitor<T> : DataTypeVisitor<JsonWriter, Any?, T, Unit> {
    override fun JsonWriter.simple(arg: Any?, raw: DataType<T>, kind: DataType.Simple.Kind) {
        if (raw is DataType.Nullable<*> && arg === null) nullValue()
        else {
            @Suppress("IMPLICIT_CAST_TO_ANY") when (kind) {
                DataType.Simple.Kind.Bool -> value(arg as Boolean)
                DataType.Simple.Kind.I8 -> value((arg as Byte).toInt())
                DataType.Simple.Kind.I16 -> value((arg as Short).toInt())
                DataType.Simple.Kind.I32 -> value(arg as Int)
                DataType.Simple.Kind.I64 -> value(arg as Long)
                DataType.Simple.Kind.F32 -> value(arg as Float)
                DataType.Simple.Kind.F64 -> value(arg as Double)
                DataType.Simple.Kind.Str -> value(arg as String)
                DataType.Simple.Kind.Blob -> value(Base64.encodeToString(arg as ByteArray, Base64.DEFAULT))
            }.also { }
        }
    }

    override fun <E> JsonWriter.collection(arg: Any?, raw: DataType<T>, type: DataType.Collect<T, E>) {
        if (arg === null) nullValue() // Nullable.encode is null->null, skip it
        else type.elementType.let { elType ->
            beginArray()
            (arg as Collection<Any?>).forEach { writeEncoded(elType, it) }
            endArray()
        }
    }
}
