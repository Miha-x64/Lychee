package net.aquadc.persistence.android

import net.aquadc.persistence.android.json.json
import net.aquadc.persistence.android.json.tokens
import net.aquadc.persistence.android.json.writeTo
import net.aquadc.persistence.extended.Tuple
import net.aquadc.persistence.extended.Tuple3
import net.aquadc.persistence.extended.build
import net.aquadc.persistence.extended.buildPartial
import net.aquadc.persistence.extended.either.EitherLeft
import net.aquadc.persistence.extended.either.EitherRight
import net.aquadc.persistence.extended.either.either
import net.aquadc.persistence.extended.partial
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.struct.build
import net.aquadc.persistence.tokens.readAs
import net.aquadc.persistence.tokens.readListOf
import net.aquadc.persistence.tokens.tokens
import net.aquadc.persistence.tokens.tokensFrom
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.byteString
import net.aquadc.persistence.type.collection
import net.aquadc.persistence.type.double
import net.aquadc.persistence.type.enumSet
import net.aquadc.persistence.type.int
import net.aquadc.persistence.type.long
import net.aquadc.persistence.type.nullable
import net.aquadc.persistence.type.serialized
import net.aquadc.persistence.type.set
import net.aquadc.persistence.type.string
import net.aquadc.properties.persistence.enum
import okio.ByteString.Companion.decodeHex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test
import java.io.StringWriter
import java.util.EnumSet


/**
 * Copy of [net.aquadc.persistence.struct.StructTests] test.
 */
class PersistenceTest {

    enum class SomeEnum {
        A, B, C, D;
        companion object {
            val Type = enum<SomeEnum>()
            val SetType = enumSet(Type)
            val BitmaskType = enumSet(long, SomeEnum::ordinal)
        }
    }

    object Sch : Schema<Sch>() {
        val tupleType = Tuple("a", long, "b", double)

        val INT = "int" let int
        val DOUBLE = "double" let double
        val ENUM = "enum" let SomeEnum.Type
        val ENUM_SET = "enumSet" let SomeEnum.SetType
        val ENUM_SET_BITMASK = "enumSetBitmask" let SomeEnum.BitmaskType
        val ENUM_SET_COLLECTION = "enumSetCollection" let collection(nullable(SomeEnum.SetType))
        val STRING = "string" let string
        val BYTES = "bytes" let serialized(set(int))
        val BLOB = "blob" let byteString
        val STRUCT = "struct" let nullable(Sch)
        val PART = "part" let partial(Sch)
        val EITHER = "either" let either("left", tupleType, "right", int)
    }

    val instance = Sch.build {
        it[INT] = 42
        it[DOUBLE] = 42.0
        it[ENUM] = SomeEnum.C
        it[ENUM_SET] = setOf(SomeEnum.C, SomeEnum.D)
        it[ENUM_SET_BITMASK] = setOf(SomeEnum.A, SomeEnum.D)
        it[ENUM_SET_COLLECTION] = listOf(null, emptySet(), setOf(SomeEnum.A, SomeEnum.B), null, setOf())
        it[STRING] = "forty-two"
        it[BYTES] = setOf(1, 2, 4)
        it[BLOB] = "ADD1C7ED".decodeHex()
        it[STRUCT] = Sch.build {
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
            it[PART] = Sch.buildPartial { }
            it[EITHER] = EitherLeft(tupleType.build(10, 20.0))
        }
        it[PART] = Sch.buildPartial {
            it[STRING] = "I'm partial!"
        }
        it[EITHER] = EitherRight(14)
    }

    @Test fun `json object`() {
        val json = StringWriter().also { instance.tokens().writeTo(it.json()) }.toString()
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
                            .tokensFrom(schema.build("lorem", innerSchema.build("ipsum", "dolor")))
                            .writeTo(it.json())
                }.toString()
        )
    }

    val smallSchema = Tuple3("a", string, "b", string, "c", string)
    val partialSmallSchema = partial(smallSchema)

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
                write(smallSchema, smallSchema.build("lorem", "ipsum", "dolor"))
        )
        assertEquals(
                """{"a":"lorem","b":"ipsum","c":"dolor"}""",
                write(partialSmallSchema, smallSchema.buildPartial("lorem", "ipsum", "dolor"))
        )

        assertEquals(
                smallSchema.build("lorem", "ipsum", "dolor"),
                read("""{"a":"lorem","b":"ipsum","c":"dolor"}""", smallSchema)
        )
        assertEquals(
                smallSchema.buildPartial("lorem", "ipsum", "dolor"),
                read("""{"a":"lorem","b":"ipsum","c":"dolor"}""", smallSchema)
        )
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
