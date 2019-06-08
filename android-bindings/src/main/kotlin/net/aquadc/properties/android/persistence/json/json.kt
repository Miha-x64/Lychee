@file:JvmName("Json")
package net.aquadc.properties.android.persistence.json

import android.util.Base64
import android.util.JsonReader
import android.util.JsonToken
import android.util.JsonWriter
import net.aquadc.persistence.each
import net.aquadc.persistence.fatAsList
import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.FieldSet
import net.aquadc.persistence.struct.PartialStruct
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.struct.allFieldSet
import net.aquadc.persistence.struct.emptyFieldSet
import net.aquadc.persistence.struct.forEach
import net.aquadc.persistence.struct.plus
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.DataTypeVisitor
import net.aquadc.persistence.type.match
import net.aquadc.properties.android.persistence.assertFitsByte
import net.aquadc.properties.android.persistence.assertFitsShort


/**
 * Reads a JSON 'array' of values denoted by [type] as a list of [T]s,
 * consuming both opening and closing square braces.
 * Each value is read using [read].
 */
fun <T> JsonReader.readListOf(type: DataType<T>): List<T> {
    // TODO: when [type] is primitive, use specialized collections

    beginArray()
    val list = if (!hasNext()) emptyList() else {
        val first = read(type)

        if (!hasNext()) listOf(first) else {
            val list = ArrayList<T>()
            list.add(first)

            do list.add(read(type))
            while (hasNext())

            list
        }
    }
    endArray()

    return list
}

/**
 * Reads a JSON value denoted by [type] as [T].
 *
 * For [Schema]s and [DataType.Partial]s,
 * this ignores key-value pairs not listed in schema,
 * and consumes both opening and closing curly braces.
 * Throws an exception if there was no value for any [FieldDef] without a default value,
 * or if [JsonReader] met unexpected token for the given [FieldDef.type].
 */
fun <T> JsonReader.read(type: DataType<T>): T =
        (readerVis as JsonReaderVisitor<T>).match(type, this, null)

private val readerVis = JsonReaderVisitor<Any?>()

private class JsonReaderVisitor<T> : DataTypeVisitor<JsonReader, Nothing?, T, T> {

    override fun JsonReader.simple(arg: Nothing?, nullable: Boolean, type: DataType.Simple<T>): T =
            if (nullable && peek() === JsonToken.NULL) {
                skipValue()
                null as T
            } else type.load(when (type.kind) {
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

    override fun <E> JsonReader.collection(arg: Nothing?, nullable: Boolean, type: DataType.Collect<T, E>): T =
            if (nullable && peek() === JsonToken.NULL) {
                skipValue()
                null as T
            } else type.load(readListOf(type.elementType))

    override fun <SCH : Schema<SCH>> JsonReader.partial(arg: Nothing?, nullable: Boolean, type: DataType.Partial<T, SCH>): T =
            when (val actual = peek()) {
                JsonToken.NULL -> {
                    check(nullable)
                    skipValue()
                    null as T
                }
                JsonToken.BEGIN_ARRAY -> { // treat [] as {}, if you were unlucky to deal with PHP server-side
                    check(isLenient) { "expected object, was array. Set lenient=true to treat empty arrays as empty objects" }
                    beginArray()
                    endArray() // crash on nonempty arrays
                    type.load(emptyFieldSet(), null)
                }
                JsonToken.BEGIN_OBJECT -> {
                    beginObject()
                    var fields = emptyFieldSet<SCH, FieldDef<SCH, *>>()
                    var values: Array<Any?>? = null
                    if (hasNext()) {
                        val byName = type.schema.fieldsByName
                        values = arrayOfNulls(byName.size)
                        do {
                            val field = byName[nextName()]
                            if (field == null) skipValue() // unsupported value
                            else {
                                val oldFields = fields
                                fields += field
                                if (oldFields.bitmask == fields.bitmask) {
                                    throw UnsupportedOperationException("duplicate name in JSON object: ${field.name}")
                                }
                                values[field.ordinal.toInt()] = read(field.type)
                            }
                        } while (hasNext())
                    }
                    endObject()
                    type.load(fields, values)
                }
                else -> {
                    val expect = arrayListOf("object")
                    if (isLenient) expect.add("empty array")
                    if (nullable) expect.add("null")
                    error(expect.joinToString(prefix = "expected ", postfix = ", was $actual"))
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
    list.each { write(it, fields) }
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

/**
 * Writes a value denoted by [type].
 */
fun <T> JsonWriter.write(type: DataType<T>, value: T): Unit =
        (writerVis as JsonWriterVisitor<T>).match(type, this, value)

@Suppress("NOTHING_TO_INLINE") // just capture T and assert value is present
private inline fun <SCH : Schema<SCH>, T> JsonWriter.writeValueFrom(struct: PartialStruct<SCH>, field: FieldDef<SCH, T>) =
        write(field.type, struct.getOrThrow(field))

private val writerVis = JsonWriterVisitor<Any?>()

private class JsonWriterVisitor<T> : DataTypeVisitor<JsonWriter, T, T, Unit> {
    override fun JsonWriter.simple(arg: T, nullable: Boolean, type: DataType.Simple<T>) {
        if (nullable && arg === null) nullValue()
        else {
            val arg = type.store(arg)
            when (type.kind) {
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

    override fun <E> JsonWriter.collection(arg: T, nullable: Boolean, type: DataType.Collect<T, E>) {
        if (nullable && arg === null) nullValue() // Nullable.encode is null->null, skip it
        else type.elementType.let { elType ->
            beginArray()
            type.store(arg).fatAsList<Any?>().each { write(elType, it as E) }
            // TODO: when [type] is primitive and [arg] is a primitive array, avoid boxing
            endArray()
        }
    }

    override fun <SCH : Schema<SCH>> JsonWriter.partial(arg: T, nullable: Boolean, type: DataType.Partial<T, SCH>) {
        if (nullable && arg === null) nullValue()
        else {
            val partial = type.store(arg)
            beginObject()
            partial.schema.forEach<SCH, FieldDef<SCH, *>>(partial.fields) { field ->
                name(field.name)
                writeValueFrom(partial, field)
            }
            endObject()
        }
    }
}
