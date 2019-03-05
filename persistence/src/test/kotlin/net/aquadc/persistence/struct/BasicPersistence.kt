package net.aquadc.persistence.struct

import net.aquadc.persistence.stream.DataStreams
import net.aquadc.persistence.stream.read
import net.aquadc.persistence.stream.write
import net.aquadc.persistence.type.byteString
import net.aquadc.persistence.type.collection
import net.aquadc.persistence.type.double
import net.aquadc.persistence.type.enum
import net.aquadc.persistence.type.enumSet
import net.aquadc.persistence.type.int
import net.aquadc.persistence.type.long
import net.aquadc.persistence.type.nullable
import net.aquadc.persistence.type.serialized
import net.aquadc.persistence.type.set
import net.aquadc.persistence.type.string
import okio.ByteString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream


class BasicPersistence {

    enum class SomeEnum {
        A, B, C, D;
        companion object {
            val Type = enum(enumValues(), string, SomeEnum::name)
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
    }

    @Test(expected = NoSuchElementException::class) fun noDefault() {
        Sch.build { }
    }

    @Test fun streams() {
        val serialized = ByteArrayOutputStream().also { DataStreams.write(DataOutputStream(it), instance) }.toByteArray()
        val deserialized = DataStreams.read(DataInputStream(ByteArrayInputStream(serialized)), Sch)
        with(Sch) { arrayOf(INT, DOUBLE, ENUM, ENUM_SET, ENUM_SET_COLLECTION, STRING) }.forEach { field ->
            val orig = instance[field]
            val copy = deserialized[field]
            assertEquals(orig, copy)
            if (orig::class.javaPrimitiveType === null && !orig.javaClass.isEnum)
                assertNotSame(field.toString(), orig, copy)
        }
        assertEquals(instance, deserialized)
        assertNotSame(instance, deserialized)
        assertEquals(instance.hashCode(), deserialized.hashCode())
        assertEquals(instance.toString(), deserialized.toString()) // hmm... this relies on objects order inside a set
    }

    @Test fun copy() {
        assertEquals(Sch.build {
            it[INT] = 42
            it[DOUBLE] = 100500.0
            it[ENUM] = SomeEnum.C
            it[ENUM_SET] = setOf(SomeEnum.C, SomeEnum.D)
            it[ENUM_SET_BITMASK] = setOf(SomeEnum.A, SomeEnum.D)
            it[ENUM_SET_COLLECTION] = listOf(null, emptySet(), setOf(SomeEnum.A, SomeEnum.B), null, setOf())
            it[STRING] = "forty-two"
            it[BYTES] = setOf(1, 2, 4)
            it[BLOB] = ByteString.EMPTY
        }, instance.copy {
            it[DOUBLE] = 100500.0
            it[BLOB] = ByteString.EMPTY
        })
    }

}
