package net.aquadc.persistence.extended

import net.aquadc.persistence.struct.StructSnapshot
import net.aquadc.persistence.struct.allFieldSet
import net.aquadc.persistence.struct.asFieldSet
import net.aquadc.persistence.struct.build
import net.aquadc.persistence.struct.plus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test


class PartialsTest {

    @Test(expected = NoSuchElementException::class)
    fun `require absent`() {
        SomeSchema.buildPartial { }.getOrThrow(SomeSchema.A)
    }
    @Test fun absent() {
        assertEquals(null, SomeSchema.buildPartial { }.getOrNull(SomeSchema.A))
    }

    @Test fun partial() {
        val p = SomeSchema.buildPartial {
            it[A] = "123"
            it[B] = 99
        }
        assertEquals("123", p.getOrThrow(SomeSchema.A))
        assertEquals(99, p.getOrThrow(SomeSchema.B))
        assertEquals(-1L, p.getOrDefault(SomeSchema.C, -1L))
        assertEquals(SomeSchema.A + SomeSchema.B, p.fields)
    }

    @Test fun full() {
        val p = SomeSchema.buildPartial {
            it[A] = "123"
            it[B] = 99
            it[C] = 100500L
        }
        assertEquals("123", p.getOrThrow(SomeSchema.A))
        assertEquals(99, p.getOrThrow(SomeSchema.B))
        assertEquals(100500L, p.getOrThrow(SomeSchema.C))
        assertTrue(p is StructSnapshot)
        assertEquals(SomeSchema.allFieldSet(), p.fields)
    }

    @Test fun take() {
        val taken = SomeSchema.build {
            it[A] = ""
            it[B] = 1
            it[C] = 1L
        }.take(SomeSchema.A + SomeSchema.C)

        assertEquals(SomeSchema.A + SomeSchema.C, taken.fields)
        assertEquals("", taken.getOrThrow(SomeSchema.A))
        assertEquals(-1, taken.getOrElse(SomeSchema.B) { -1 })
        assertEquals(1L, taken.getOrThrow(SomeSchema.C))
    }

    @Test fun `take all`() {
        val full = SomeSchema.build {
            it[A] = ""
            it[B] = 1
            it[C] = 1L
        }

        assertSame(full, full.take(SomeSchema.allFieldSet()))
    }

    @Test fun `copy from`() {
        assertEquals(SomeSchema.buildPartial {
            it[A] = "a"
            it[B] = 1
        }, SomeSchema.buildPartial {
            it[A] = "a"
        }.copy {
            assertEquals(
                    B.asFieldSet(),
                    it.setFrom(SomeSchema.buildPartial {
                        it[B] = 1
                        it[C] = 1L
                    }, A + B)
            )
        })
    }

}
