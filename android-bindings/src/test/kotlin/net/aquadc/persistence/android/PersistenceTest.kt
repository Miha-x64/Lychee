package net.aquadc.persistence.android

import net.aquadc.persistence.android.json.json
import net.aquadc.persistence.android.json.tokens
import net.aquadc.persistence.android.json.writeTo
import net.aquadc.persistence.extended.Partial
import net.aquadc.persistence.extended.tuple.Tuple
import net.aquadc.persistence.extended.tuple.Tuple3
import net.aquadc.persistence.extended.tuple.buildPartial
import net.aquadc.persistence.extended.colour
import net.aquadc.persistence.extended.either.EitherLeft
import net.aquadc.persistence.extended.either.EitherRight
import net.aquadc.persistence.extended.either.either
import net.aquadc.persistence.extended.tuple.invoke
import net.aquadc.persistence.extended.partial
import net.aquadc.persistence.extended.tuple.times
import net.aquadc.persistence.extended.uuid
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.struct.invoke
import net.aquadc.persistence.tokens.Token
import net.aquadc.persistence.tokens.iteratorOf
import net.aquadc.persistence.tokens.iteratorOfTransient
import net.aquadc.persistence.tokens.readAs
import net.aquadc.persistence.tokens.readListOf
import net.aquadc.persistence.tokens.tokens
import net.aquadc.persistence.tokens.tokensFrom
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.byteString
import net.aquadc.persistence.type.collection
import net.aquadc.persistence.type.enumSet
import net.aquadc.persistence.type.f64
import net.aquadc.persistence.type.i32
import net.aquadc.persistence.type.i64
import net.aquadc.persistence.type.nothing
import net.aquadc.persistence.type.nullable
import net.aquadc.persistence.type.serialized
import net.aquadc.persistence.type.set
import net.aquadc.persistence.type.string
import net.aquadc.properties.function.Enumz
import net.aquadc.properties.persistence.enumByName
import okio.ByteString.Companion.decodeHex
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.StringWriter
import java.util.Base64
import java.util.EnumSet


/**
 * Copy of [net.aquadc.persistence.struct.StructTests] test.
 */
class PersistenceTest {

    enum class SomeEnum {
        A, B, C, D;
        companion object {
            val Type = enumByName<SomeEnum>()
            val SetType = enumSet(Type)
            val BitmaskType = enumSet<SomeEnum>(i64, Enumz.Ordinal)
        }
    }

    object Sch : Schema<Sch>() {
        val tupleType = i64 * f64

        val INT = "int" let i32
        val DOUBLE = "double" let f64
        val ENUM = "enum" let SomeEnum.Type
        val ENUM_SET = "enumSet" let SomeEnum.SetType
        val ENUM_SET_BITMASK = "enumSetBitmask" let SomeEnum.BitmaskType
        val ENUM_SET_COLLECTION = "enumSetCollection" let collection(nullable(SomeEnum.SetType))
        val STRING = "string" let string
        val BYTES = "bytes" let serialized(set(i32))
        val BLOB = "blob" let byteString
        val STRUCT = "struct" let nullable(Sch)
        val PART = "part" let partial(Sch)
        val EITHER = "either" let either("left", tupleType, "right", i32)
        val COLOUR = "colour" let colour
        val COLOURS = "colours" let collection(colour)
        val UUID = "uuid" let uuid
        val UUIDS = "uuids" let collection(uuid)
    }

    val instance = Sch {
        it[INT] = 42
        it[DOUBLE] = 42.0
        it[ENUM] = SomeEnum.C
        it[ENUM_SET] = setOf(SomeEnum.C, SomeEnum.D)
        it[ENUM_SET_BITMASK] = setOf(SomeEnum.A, SomeEnum.D)
        it[ENUM_SET_COLLECTION] = listOf(null, emptySet(), setOf(SomeEnum.A, SomeEnum.B), null, setOf())
        it[STRING] = "forty-two"
        it[BYTES] = setOf(1, 2, 4)
        it[BLOB] = "ADD1C7ED".decodeHex()
        it[STRUCT] = Sch {
            it[INT] = 34
            it[DOUBLE] = 98.6
            it[ENUM] = SomeEnum.A
            it[ENUM_SET] = setOf()
            it[ENUM_SET_BITMASK] = EnumSet.of(SomeEnum.D)
            it[ENUM_SET_COLLECTION] = emptyList()
            it[STRING] = "I'm a string, info 146%"
            it[BYTES] = setOf()
            it[BLOB] = "B10B".decodeHex()
            it[STRUCT] = null
            it[PART] = Sch.Partial { }
            it[EITHER] = EitherLeft(tupleType(10, 20.0))
            it[COLOUR] = 0xFFFF8845.toInt()
            it[COLOURS] = listOf()
            it[UUID] = java.util.UUID.randomUUID()
            it[UUIDS] = emptyList()
        }
        it[PART] = Sch.Partial {
            it[STRING] = "I'm partial!"
        }
        it[EITHER] = EitherRight(14)
        it[COLOUR] = 0x66666666.toInt()
        it[COLOURS] = listOf(0xFF000000.toInt())
        it[UUID] = java.util.UUID.randomUUID()
        it[UUIDS] = listOf(java.util.UUID.randomUUID())
    }

    @Test fun `json object`() {
        val json = StringWriter().also { instance.tokens().writeTo(it.json()) }.toString()
        println(json)
        val deserialized = json.reader().json().tokens().readAs(Sch)
        assertEqualToOriginal(deserialized, true)
    }

    @Test fun `empty json array`() {
        assertSame(emptyList<Nothing>(), "[]".reader().json().tokens().readListOf(string))
        assertSame(emptyList<Nothing>(), "[]".reader().json().tokens().readAs(collection(string)))
    }

    @Test fun `json string list`() {
        assertEquals(listOf("1", "22", "ttt"), """["1", "22", "ttt"]""".reader().json().tokens().readListOf(string))
        assertEquals(listOf("1", "22", "ttt"), """["1", "22", "ttt"]""".reader().json().tokens().readAs(collection(string)))

        assertEquals("""["1","22","ttt"]""", StringWriter().also { collection(string).tokensFrom(listOf("1", "22", "ttt")).writeTo(it.json()) }.toString())
    }

    @Test fun `json iterators`() {
        val empty = "[]".reader().json().tokens().iteratorOf(nothing)
        assertFalse(empty.hasNext())
        try { empty.next(); fail() }
        catch (expected: NoSuchElementException) {}

        val singleString = """["hello"]""".reader().json().tokens().iteratorOf(string)
        assertEquals("hello", singleString.next())
        try { singleString.next(); fail() }
        catch (expected: NoSuchElementException) {}

        assertEquals(
                listOf(1, 2, 4, 8, 16, 32, 64),
                "[1, 2, 4, 8, 16, 32, 64]".reader().json().tokens().iteratorOf(i32).asSequence().toList()
        )
    }

    @Test fun `json struct iterators`() {
        val s = """[{"first":"v1","second":"v2"},{"first":"v3","second":"v4"}]"""
        val schema = string * string
        val stable = s.reader().json().tokens().iteratorOf(schema)
        val transient = s.reader().json().tokens().iteratorOfTransient(schema)

        assertTrue(stable.hasNext())
        assertTrue(transient.hasNext())

        val reference0 = schema("v1", "v2")
        assertEquals(reference0, stable.next())
        assertEquals(reference0, transient.next())

        val reference1 = schema("v3", "v4")
        assertEquals(reference1, stable.next())
        assertEquals(reference1, transient.next())

        assertFalse(stable.hasNext())
        assertFalse(transient.hasNext())

        try { stable.next(); fail() } catch (expected: NoSuchElementException) {}
        try { transient.next(); fail() } catch (expected: NoSuchElementException) {}
    }

    fun assertEqualToOriginal(deserialized: Struct<Sch>, assertNotSame: Boolean) {
        with(Sch) { arrayOf(INT, DOUBLE, ENUM, ENUM_SET, ENUM_SET_BITMASK, ENUM_SET_COLLECTION, STRING, BYTES, BLOB) }.forEach { field ->
            val orig = instance[field]
            val copy = deserialized[field]
            assertEquals(orig, copy)
            if (assertNotSame && orig::class.javaPrimitiveType === null && !orig.javaClass.isEnum)
                assertNotSame(field.toString(), orig, copy)
        }
        assertEquals(instance, deserialized)
        assertNotSame(instance, deserialized)
        assertEquals(instance.hashCode(), deserialized.hashCode())
        assertEquals(
                instance.toString().let { it.substring(it.indexOf('(')) }, // drop struct type, leave contents
                deserialized.toString().let { it.substring(it.indexOf('(')) }
        ) // hmm... this relies on objects order inside a set
    }

    @Test(expected = UnsupportedOperationException::class) fun `fail on dupe JSON names`() {
        """{"int":1, "int": 2}""".reader().json().tokens().readAs(partial(Sch))
    }

    @Test fun renaming() {
        val innerSchema = Tuple("q", string, "w", string)
        val schema = Tuple("a", string, "b", innerSchema)

        assertEquals(
                """{"first":"lorem","second":{"a":"ipsum","b":"dolor"}}""",
                StringWriter().also {
                    Tuple("first", string, "second", Tuple("a", string, "b", string))
                            .tokensFrom(schema.invoke("lorem", innerSchema.invoke("ipsum", "dolor")))
                            .writeTo(it.json())
                }.toString()
        )
    }

    private val smallSchema = Tuple3("a", string, "b", string, "c", string)
    private val partialSmallSchema = partial(smallSchema)

    @Test fun `json empty partial`() {
        assertEquals(
                """{}""",
                write(partialSmallSchema, smallSchema.buildPartial())
        )
        assertEquals(
                smallSchema.buildPartial(),
                read("""{}""", partialSmallSchema)
        )
        assertEquals(
                smallSchema.buildPartial(),
                read("""[]""", partialSmallSchema, lenient = true)
        )
    }

    @Test fun `json partial with a single field`() {
        assertEquals(
                """{"a":"lorem"}""",
                write(partialSmallSchema, smallSchema.buildPartial(first = "lorem"))
        )
        assertEquals(
                """{"b":"lorem"}""",
                write(partialSmallSchema, smallSchema.buildPartial(second = "lorem"))
        )
        assertEquals(
                smallSchema.buildPartial(first = "lorem"),
                read("""{"a":"lorem"}""", partialSmallSchema)
        )
        assertEquals(
                smallSchema.buildPartial(second = "lorem"),
                read("""{"b":"lorem"}""", partialSmallSchema)
        )
    }

    @Test fun `json partial`() {
        assertEquals(
                """{"a":"lorem","b":"ipsum"}""",
                write(partialSmallSchema, smallSchema.buildPartial(first = "lorem", second = "ipsum"))
        )
        assertEquals(
                """{"a":"lorem","c":"ipsum"}""",
                write(partialSmallSchema, smallSchema.buildPartial(first = "lorem", third = "ipsum"))
        )
        assertEquals(
                """{"b":"lorem","c":"ipsum"}""",
                write(partialSmallSchema, smallSchema.buildPartial(second = "lorem", third = "ipsum"))
        )

        assertEquals(
                smallSchema.buildPartial(first = "lorem", second = "ipsum"),
                read("""{"a":"lorem","b":"ipsum"}""", partialSmallSchema)
        )
        assertEquals(
                smallSchema.buildPartial(first = "lorem", third = "ipsum"),
                read("""{"a":"lorem","c":"ipsum"}""", partialSmallSchema)
        )
        assertEquals(
                smallSchema.buildPartial(second = "lorem", third = "ipsum"),
                read("""{"b":"lorem","c":"ipsum"}""", partialSmallSchema)
        )
    }

    @Test fun `full json partial`() {
        assertEquals(
                """{"a":"lorem","b":"ipsum","c":"dolor"}""",
                write(smallSchema, smallSchema("lorem", "ipsum", "dolor"))
        )
        assertEquals(
                """{"a":"lorem","b":"ipsum","c":"dolor"}""",
                write(partialSmallSchema, smallSchema.buildPartial("lorem", "ipsum", "dolor"))
        )

        assertEquals(
                smallSchema("lorem", "ipsum", "dolor"),
                read("""{"a":"lorem","b":"ipsum","c":"dolor"}""", smallSchema)
        )
        assertEquals(
                smallSchema.buildPartial("lorem", "ipsum", "dolor"),
                read("""{"a":"lorem","b":"ipsum","c":"dolor"}""", smallSchema)
        )
    }

    @Test fun coercions() {
        val blob = byteArrayOf(1, 2, 3)
        val base = Base64.getEncoder().encodeToString(blob)
        val tokens = """{"1":"456.789","$base":"123"}""".reader().json().tokens()

        tokens.poll(Token.BeginDictionary)

        assertEquals(Token.Str, tokens.peek())
        assertEquals(1, tokens.poll(Token.I32))

        assertEquals(Token.Str, tokens.peek())
        assertEquals(456.789, tokens.poll(Token.F64))

        assertEquals(Token.Str, tokens.peek())
        assertArrayEquals(blob, tokens.poll(Token.Blob) as ByteArray)

        assertEquals(Token.Str, tokens.peek())
        assertEquals(123, tokens.poll(Token.I32))

    }

    private fun <T> read(json: String, type: DataType<T>, lenient: Boolean = false): T =
            json.reader().json().also {
                it.isLenient = lenient
            }.tokens().readAs(type)

    private fun <T> write(type: DataType<T>, value: T): String =
            StringWriter().also {
                type.tokensFrom(value).writeTo(it.json())
            }.toString()

}
