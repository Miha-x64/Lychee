package net.aquadc.properties.android.persistence

import android.util.JsonReader
import android.util.JsonWriter
import net.aquadc.persistence.extended.buildPartial
import net.aquadc.persistence.extended.partial
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.struct.build
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
import net.aquadc.properties.android.persistence.json.read
import net.aquadc.properties.android.persistence.json.write
import net.aquadc.properties.persistence.enum
import okio.ByteString
import org.junit.Assert
import org.junit.Test
import java.io.StringReader
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
        it[BLOB] = ByteString.decodeHex("ADD1C7ED")
        it[STRUCT] = Sch.build {
            it[INT] = 34
            it[DOUBLE] = 98.6
            it[ENUM] = SomeEnum.A
            it[ENUM_SET] = setOf()
            it[ENUM_SET_BITMASK] = EnumSet.of(SomeEnum.D)
            it[ENUM_SET_COLLECTION] = emptyList()
            it[STRING] = "I'm a string, info 146%"
            it[BYTES] = setOf()
            it[BLOB] = ByteString.decodeHex("B10B")
            it[STRUCT] = null
            it[PART] = Sch.buildPartial { }
        }
        it[PART] = Sch.buildPartial {
            it[STRING] = "I'm partial!"
        }
    }

    @Test fun json() {
        val json = StringWriter().also { JsonWriter(it).write(instance) }.toString()
        val deserialized = JsonReader(StringReader(json)).read(Sch)
        assertEqualToOriginal(deserialized, true)
    }

    fun assertEqualToOriginal(deserialized: Struct<Sch>, assertNotSame: Boolean) {
        with(Sch) { arrayOf(INT, DOUBLE, ENUM, ENUM_SET, ENUM_SET_BITMASK, ENUM_SET_COLLECTION, STRING, BYTES, BLOB) }.forEach { field ->
            val orig = instance[field]
            val copy = deserialized[field]
            Assert.assertEquals(orig, copy)
            if (assertNotSame && orig::class.javaPrimitiveType === null && !orig.javaClass.isEnum)
                Assert.assertNotSame(field.toString(), orig, copy)
        }
        Assert.assertEquals(instance, deserialized)
        Assert.assertNotSame(instance, deserialized)
        Assert.assertEquals(instance.hashCode(), deserialized.hashCode())
        Assert.assertEquals(
                instance.toString().let { it.substring(it.indexOf('(')) }, // drop struct type, leave contents
                deserialized.toString().let { it.substring(it.indexOf('(')) }
        ) // hmm... this relies on objects order inside a set
    }

}
