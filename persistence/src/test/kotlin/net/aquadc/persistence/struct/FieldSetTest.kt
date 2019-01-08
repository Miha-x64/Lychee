package net.aquadc.persistence.struct

import net.aquadc.properties.testing.SomeSchema
import org.junit.Assert.*
import org.junit.Test

class FieldSetTest {

    // TODO: should test 64-field schema!

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


    @Test fun allEach() {
        val list = ArrayList<FieldDef<SomeSchema, *>>()
        SomeSchema.forEach(SomeSchema.A + SomeSchema.B + SomeSchema.C, { list.add(it) })
        assertEquals(listOf(SomeSchema.A, SomeSchema.B, SomeSchema.C), list)
    }

    @Test fun allContain() {
        val set = SomeSchema.allFieldSet()
        assertTrue(SomeSchema.A in set)
        assertTrue(SomeSchema.B in set)
        assertTrue(SomeSchema.C in set)
    }


    @Test fun all() {
        assertEquals(SomeSchema.allFieldSet(), SomeSchema.A + SomeSchema.B + SomeSchema.C)
        assertEquals(SomeSchema.immutableFieldSet(), SomeSchema.A + SomeSchema.B)
        assertEquals(SomeSchema.mutableFieldSet(), SomeSchema.C.asFieldSet())
    }

}
