package net.aquadc.persistence.struct

import net.aquadc.persistence.stream.DataStreams
import net.aquadc.persistence.stream.read
import net.aquadc.persistence.stream.write
import net.aquadc.persistence.type.byteString
import net.aquadc.persistence.type.collection
import net.aquadc.persistence.type.f64
import net.aquadc.persistence.type.enum
import net.aquadc.persistence.type.enumSet
import net.aquadc.persistence.type.i32
import net.aquadc.persistence.type.i64
import net.aquadc.persistence.type.nullable
import net.aquadc.persistence.type.serialized
import net.aquadc.persistence.type.set
import net.aquadc.persistence.type.string
import okio.ByteString
import okio.ByteString.Companion.decodeHex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.EnumSet


class StructTests {

    enum class SomeEnum {
        A, B, C, D;
        companion object {
            val Type = enum(enumValues(), string, SomeEnum::name)
            val SetType = enumSet(Type)
            val BitmaskType = enumSet(i64, SomeEnum::ordinal)
        }
    }

    object Sch : Schema<Sch>() {
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
        }
    }

    @Test(expected = NoSuchElementException::class) fun noDefault() {
        Sch { }
    }

    @Test fun streams() {
        val serialized = ByteArrayOutputStream().also { DataStreams.write(DataOutputStream(it), instance) }.toByteArray()
        val deserialized = DataStreams.read(DataInputStream(ByteArrayInputStream(serialized)), Sch)
        with(Sch) { arrayOf(INT, DOUBLE, ENUM, ENUM_SET, ENUM_SET_BITMASK, ENUM_SET_COLLECTION, STRING, BYTES, BLOB) }.forEach { field ->
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
        assertEquals(Sch {
            it[INT] = 42
            it[DOUBLE] = 100500.0
            it[ENUM] = SomeEnum.C
            it[ENUM_SET] = setOf(SomeEnum.C, SomeEnum.D)
            it[ENUM_SET_BITMASK] = setOf(SomeEnum.A, SomeEnum.D)
            it[ENUM_SET_COLLECTION] = listOf(null, emptySet(), setOf(SomeEnum.A, SomeEnum.B), null, setOf())
            it[STRING] = "forty-two"
            it[BYTES] = setOf(1, 2, 4)
            it[BLOB] = ByteString.EMPTY
            it[STRUCT] = instance[STRUCT]
        }, instance.copy {
            it[DOUBLE] = 100500.0
            it[BLOB] = ByteString.EMPTY
        })
    }

    @Test fun defaults() {
        val struct = SomeSchema {
            it[A] = "zzz"
            it[C] = 111L
        }
        assertEquals("zzz", struct[SomeSchema.A])
        assertEquals(10, struct[SomeSchema.B])
        assertEquals(111L, struct[SomeSchema.C])
        assertEquals(92L, struct[SomeSchema.D])
    }

    @Test fun `copy from`() {
        val aaa = SomeSchema {
            it[A] = "a"
            it[B] = 1
            it[C] = 1L
        }

        val bbb = SomeSchema {
            it[A] = "b"
            it[B] = 2
            it[C] = 2L
        }

        val mix = aaa.copy {
            assertEquals(A + B, it.setFrom(bbb, A + B))
        }

        assertEquals(SomeSchema {
            it[A] = "b"
            it[B] = 2
            it[C] = 1L
        }, mix)
    }

}
