package net.aquadc.persistence.struct

import org.junit.Assert.*
import org.junit.Test

class FieldSetTest {

    @Test fun `empty forEach`() {
        SomeSchema.forEach(emptyFieldSet()) {
            fail()
        }
    }
    @Test fun `empty contains`() {
        assertFalse(SomeSchema.A in emptyFieldSet<SomeSchema>())
        assertFalse(SomeSchema.B in emptyFieldSet<SomeSchema>())
        assertFalse(SomeSchema.C in emptyFieldSet<SomeSchema>())
    }
    @Test fun `size of empty`() {
        assertEquals(0, emptyFieldSet<SomeSchema>().size)
    }
    @Test fun `empty indexOf`() {
        assertEquals(-1, emptyFieldSet<SomeSchema>().indexOf(SomeSchema.A))
        assertEquals(-1, emptyFieldSet<SomeSchema>().indexOf(SomeSchema.B))
        assertEquals(-1, emptyFieldSet<SomeSchema>().indexOf(SomeSchema.C))
    }
    @Test fun `empty minus`() {
        /*val empty = emptyFieldSet<SomeSchema>()
        assertEquals(empty, empty - empty)
        assertEquals(empty, empty - SomeSchema.A.asFieldSet())
        assertEquals(empty, empty - SomeSchema.B.asFieldSet())
        assertEquals(empty, empty - SomeSchema.C.asFieldSet())
        assertEquals(empty, empty - (SomeSchema.A + SomeSchema.B))
        assertEquals(empty, empty - SomeSchema.allFieldSet)*/
    }
    @Test fun `empty intersect`() {
        assertEquals(emptyFieldSet<SomeSchema>(), emptyFieldSet<SomeSchema>() intersect emptyFieldSet())
    }


    @Test fun `single forEach`() {
        /*val list = ArrayList<NamedLens<SomeSchema, *, *, *, *>>()
        SomeSchema.forEach_(SomeSchema.B.asFieldSet(), { list.add(it) })
        assertEquals(listOf(SomeSchema.B), list)
        assertEquals(SomeSchema.B, SomeSchema.single_(SomeSchema.B.asFieldSet()))*/
    }
    @Test fun `single contains`() {
        /*val set = SomeSchema.B.asFieldSet()
        assertFalse(SomeSchema.A in set)
        assertTrue(SomeSchema.B in set)
        assertFalse(SomeSchema.C in set)*/
    }
    @Test fun `size of single`() {
//        assertEquals(1, SomeSchema.B.asFieldSet().size)
    }
    @Test fun `single indexOf`() {
        /*val set = SomeSchema.B.upcast().asFieldSet<SomeSchema, FieldDef<SomeSchema, *, *>>()
        assertEquals(-1, set.indexOf(SomeSchema.A))
        assertEquals(0, set.indexOf(SomeSchema.B))
        assertEquals(-1, set.indexOf(SomeSchema.C))*/
    }
    @Test fun `single minus`() {
        /*val empty = emptyFieldSet<SomeSchema>()
        assertEquals(SomeSchema.A.asFieldSet(), SomeSchema.A.asFieldSet() - empty)
        assertEquals(empty, SomeSchema.A.asFieldSet() - SomeSchema.A)
        assertEquals(SomeSchema.A.asFieldSet(), SomeSchema.A.asFieldSet() - SomeSchema.B)
        assertEquals(SomeSchema.A.asFieldSet(), SomeSchema.A + SomeSchema.B - SomeSchema.B)*/
    }
    @Test fun `single intersect`() {
        /*assertEquals(emptyFieldSet<SomeSchema>(), SomeSchema.A.asFieldSet() intersect emptyFieldSet())
        assertEquals(emptyFieldSet<SomeSchema>(), SomeSchema.A.asFieldSet() intersect SomeSchema.B.asFieldSet())*/
    }


    @Test fun `two forEach`() {
        /*val list = ArrayList<NamedLens<SomeSchema, *, *, *, *>>()
        SomeSchema.forEach_(SomeSchema.A + SomeSchema.B, { list.add(it) })
        assertEquals(listOf(SomeSchema.A, SomeSchema.B), list)*/
    }
    @Test fun `two contain`() {
        /*val set = SomeSchema.B + SomeSchema.C
        assertFalse(SomeSchema.A in set)
        assertTrue(SomeSchema.B in set)
        assertTrue(SomeSchema.C in set)*/
    }
    @Test fun `two size`() {
//        assertEquals(2, (SomeSchema.A + SomeSchema.B).size)
    }
    @Test fun `two indexOf`() {
        /*val set = SomeSchema.A + SomeSchema.B
        assertEquals(0, set.indexOf(SomeSchema.A))
        assertEquals(1, set.indexOf(SomeSchema.B))
        assertEquals(-1, set.indexOf(SomeSchema.C))*/
    }
    @Test fun `two minus`() {
        /*assertEquals(SomeSchema.A + SomeSchema.B, SomeSchema.A + SomeSchema.B - SomeSchema.C)
        assertEquals(SomeSchema.A + SomeSchema.B, SomeSchema.A + SomeSchema.B + SomeSchema.C - SomeSchema.C)*/
    }
    @Test fun `two intersect`() {
        /*assertEquals(emptyFieldSet<SomeSchema>(), SomeSchema.A.asFieldSet() intersect SomeSchema.B.asFieldSet())
        assertEquals(SomeSchema.A.asFieldSet(), SomeSchema.A.asFieldSet() intersect SomeSchema.A.asFieldSet())*/
    }


    @Test fun `several intersect`() {
        /*assertEquals(SomeSchema.A.asFieldSet(), SomeSchema.A + SomeSchema.C intersect (SomeSchema.A + SomeSchema.B))
        assertEquals(SomeSchema.A + SomeSchema.B, SomeSchema.allFieldSet intersect (SomeSchema.A + SomeSchema.B))*/
    }


    @Test fun `all forEach`() {
        /*val list = ArrayList<FieldDef<SomeSchema, *, *>>()
        SomeSchema.forEach(SomeSchema.A + SomeSchema.B + SomeSchema.C, { list.add(it) })
        assertEquals(listOf(SomeSchema.A, SomeSchema.B, SomeSchema.C), list)*/
    }

    @Test fun `all63 forEach`() {
        val list = ArrayList<FieldDef<Schema63, *, *>>()
        Schema63.forEach(Schema63.allFieldSet) { list.add(it) }
        assertEquals(63, list.size)
    }
    @Test fun `all63 size`() {
        assertEquals(63, Schema63.allFieldSet.size)
    }
    @Test fun `all minus`() {
//        assertEquals(SomeSchema.A + SomeSchema.B + SomeSchema.D, SomeSchema.allFieldSet - SomeSchema.C)
    }

    @Test fun `all64 forEach`() {
        val list = ArrayList<FieldDef<Schema64, *, *>>()
        Schema64.forEach(Schema64.allFieldSet, { list.add(it) })
        assertEquals(64, list.size)
    }
    @Test fun `all64 size`() {
        assertEquals(64, Schema64.allFieldSet.size)
    }

    @Test fun `all contain`() {
        val set = SomeSchema.allFieldSet
        assertTrue(SomeSchema.A in set)
        assertTrue(SomeSchema.B in set)
        assertTrue(SomeSchema.C in set)
    }

    @Test fun `all63 contain`() {
        val set = Schema63.allFieldSet
        Schema63.forEach(Schema63.allFieldSet) { field ->
            assertTrue(field in set)
        }
    }

    @Test fun `all64 contain`() {
        val set = Schema64.allFieldSet
        Schema64.forEach(Schema64.allFieldSet) { field ->
            assertTrue(field in set)
        }
    }


    @Test fun all() {
        /*assertEquals(SomeSchema.allFieldSet, SomeSchema.A + SomeSchema.B + SomeSchema.C + SomeSchema.D)
        assertEquals(4, SomeSchema.allFieldSet.size)

        assertEquals(SomeSchema.immutableFieldSet, SomeSchema.A + SomeSchema.D)
        assertEquals(2, SomeSchema.immutableFieldSet.size)

        assertEquals(SomeSchema.mutableFieldSet, SomeSchema.B + SomeSchema.C)
        assertEquals(2, SomeSchema.mutableFieldSet.size)*/
    }

    @Test fun all63() {
        assertEquals(0x7FFFFFFFFFFFFFFFL, Schema63.allFieldSet.bitSet)
        assertEquals(0x7FFFFFFFFFFFFFFFL, Schema63.immutableFieldSet.bitSet)
        assertEquals(0L, Schema63.mutableFieldSet.bitSet)
    }

    @Test fun all64() {
        assertEquals(-1L, Schema64.allFieldSet.bitSet)
        assertEquals(-1L, Schema64.immutableFieldSet.bitSet)
        assertEquals(0L, Schema64.mutableFieldSet.bitSet)
    }

}
