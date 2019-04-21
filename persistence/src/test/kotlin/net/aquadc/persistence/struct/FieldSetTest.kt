package net.aquadc.persistence.struct

import org.junit.Assert.*
import org.junit.Test

class FieldSetTest {

    @Test fun emptyEach() {
        SomeSchema.forEach(emptyFieldSet()) {
            fail()
        }
    }
    @Test fun emptyContains() {
        assertFalse(SomeSchema.A in emptyFieldSet<SomeSchema, FieldDef<SomeSchema, *>>())
        assertFalse(SomeSchema.B in emptyFieldSet<SomeSchema, FieldDef<SomeSchema, *>>())
        assertFalse(SomeSchema.C in emptyFieldSet<SomeSchema, FieldDef<SomeSchema, *>>())
    }
    @Test fun emptySize() {
        assertEquals(0.toByte(), emptyFieldSet<SomeSchema, FieldDef<SomeSchema, *>>().size)
    }
    @Test fun emptyIdxOf() {
        assertEquals(-1, emptyFieldSet<SomeSchema, FieldDef<SomeSchema, *>>().indexOf(SomeSchema.A).toInt())
        assertEquals(-1, emptyFieldSet<SomeSchema, FieldDef<SomeSchema, *>>().indexOf(SomeSchema.B).toInt())
        assertEquals(-1, emptyFieldSet<SomeSchema, FieldDef<SomeSchema, *>>().indexOf(SomeSchema.C).toInt())
    }
    @Test fun emptyMinus() {
        val empty = emptyFieldSet<SomeSchema, FieldDef<SomeSchema, *>>()
        assertEquals(empty, empty - empty)
        assertEquals(empty, empty - SomeSchema.A.asFieldSet())
        assertEquals(empty, empty - SomeSchema.B.asFieldSet())
        assertEquals(empty, empty - SomeSchema.C.asFieldSet())
        assertEquals(empty, empty - (SomeSchema.A + SomeSchema.B))
        assertEquals(empty, empty - SomeSchema.allFieldSet())
    }


    @Test fun singleEach() {
        val list = ArrayList<FieldDef<SomeSchema, *>>()
        SomeSchema.forEach(SomeSchema.B.asFieldSet(), { list.add(it) })
        assertEquals(listOf(SomeSchema.B), list)
    }
    @Test fun singleContains() {
        val set = SomeSchema.B.asFieldSet()
        assertFalse(SomeSchema.A in set)
        assertTrue(SomeSchema.B in set)
        assertFalse(SomeSchema.C in set)
    }
    @Test fun singleSize() {
        assertEquals(1.toByte(), SomeSchema.B.asFieldSet().size)
    }
    @Test fun singleIdxOf() {
        val set = SomeSchema.B.asFieldSet<SomeSchema, FieldDef<SomeSchema, *>>()
        assertEquals(-1, set.indexOf(SomeSchema.A).toInt())
        assertEquals(0, set.indexOf(SomeSchema.B).toInt())
        assertEquals(-1, set.indexOf(SomeSchema.C).toInt())
    }
    @Test fun singleMinus() {
        val empty = emptyFieldSet<SomeSchema, FieldDef<SomeSchema, *>>()
        assertEquals(SomeSchema.A.asFieldSet(), SomeSchema.A.asFieldSet() - empty)
        assertEquals(empty, SomeSchema.A.asFieldSet() - SomeSchema.A)
        assertEquals(SomeSchema.A.asFieldSet(), SomeSchema.A.asFieldSet() - SomeSchema.B)
        assertEquals(SomeSchema.A.asFieldSet(), SomeSchema.A + SomeSchema.B - SomeSchema.B)
    }


    @Test fun twoEach() {
        val list = ArrayList<FieldDef<SomeSchema, *>>()
        SomeSchema.forEach(SomeSchema.A + SomeSchema.B, { list.add(it) })
        assertEquals(listOf(SomeSchema.A, SomeSchema.B), list)
    }
    @Test fun twoContain() {
        val set = SomeSchema.B + SomeSchema.C
        assertFalse(SomeSchema.A in set)
        assertTrue(SomeSchema.B in set)
        assertTrue(SomeSchema.C in set)
    }
    @Test fun twoSize() {
        assertEquals(2.toByte(), (SomeSchema.A + SomeSchema.B).size)
    }
    @Test fun twoIdxOf() {
        val set = SomeSchema.A.plus<SomeSchema, FieldDef<SomeSchema, *>>(SomeSchema.B)
        assertEquals(0, set.indexOf(SomeSchema.A).toInt())
        assertEquals(1, set.indexOf(SomeSchema.B).toInt())
        assertEquals(-1, set.indexOf(SomeSchema.C).toInt())
    }
    @Test fun twoMinus() {
        assertEquals(SomeSchema.A + SomeSchema.B, SomeSchema.A + SomeSchema.B - SomeSchema.C)
        assertEquals(SomeSchema.A + SomeSchema.B, SomeSchema.A + SomeSchema.B + SomeSchema.C - SomeSchema.C)
    }


    @Test fun allEach() {
        val list = ArrayList<FieldDef<SomeSchema, *>>()
        SomeSchema.forEach(SomeSchema.A + SomeSchema.B + SomeSchema.C, { list.add(it) })
        assertEquals(listOf(SomeSchema.A, SomeSchema.B, SomeSchema.C), list)
    }

    @Test fun allEach63() {
        val list = ArrayList<FieldDef<Schema63, *>>()
        Schema63.forEach(Schema63.allFieldSet(), { list.add(it) })
        assertEquals(63, list.size)
    }
    @Test fun all63Size() {
        assertEquals(63.toByte(), Schema63.allFieldSet().size)
    }
    @Test fun allMinus() {
        assertEquals(SomeSchema.A + SomeSchema.B, SomeSchema.allFieldSet() - SomeSchema.C)
    }

    @Test fun allEach64() {
        val list = ArrayList<FieldDef<Schema64, *>>()
        Schema64.forEach(Schema64.allFieldSet(), { list.add(it) })
        assertEquals(64, list.size)
    }
    @Test fun all64Size() {
        assertEquals(64.toByte(), Schema64.allFieldSet().size)
    }

    @Test fun allContain() {
        val set = SomeSchema.allFieldSet()
        assertTrue(SomeSchema.A in set)
        assertTrue(SomeSchema.B in set)
        assertTrue(SomeSchema.C in set)
    }

    @Test fun allContain63() {
        val set = Schema63.allFieldSet()
        Schema63.fields.forEach { field ->
            assertTrue(field in set)
        }
    }

    @Test fun allContain64() {
        val set = Schema64.allFieldSet()
        Schema64.fields.forEach { field ->
            assertTrue(field in set)
        }
    }


    @Test fun all() {
        assertEquals(SomeSchema.allFieldSet(), SomeSchema.A + SomeSchema.B + SomeSchema.C)
        assertEquals(3.toByte(), SomeSchema.allFieldSet().size)

        assertEquals(SomeSchema.immutableFieldSet(), SomeSchema.A + SomeSchema.B)
        assertEquals(2.toByte(), SomeSchema.immutableFieldSet().size)

        assertEquals(SomeSchema.mutableFieldSet(), SomeSchema.C.asFieldSet())
        assertEquals(1.toByte(), SomeSchema.mutableFieldSet().size)
    }

    @Test fun all63() {
        assertEquals(0x7FFFFFFFFFFFFFFFL, Schema63.allFieldSet().bitmask)
        assertEquals(0x7FFFFFFFFFFFFFFFL, Schema63.immutableFieldSet().bitmask)
        assertEquals(0L, Schema63.mutableFieldSet().bitmask)
    }

    @Test fun all64() {
        assertEquals(-1L, Schema64.allFieldSet().bitmask)
        assertEquals(-1L, Schema64.immutableFieldSet().bitmask)
        assertEquals(0L, Schema64.mutableFieldSet().bitmask)
    }

}
