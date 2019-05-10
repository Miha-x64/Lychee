package net.aquadc.persistence.extended

import net.aquadc.persistence.struct.StructSnapshot
import net.aquadc.persistence.struct.allFieldSet
import net.aquadc.persistence.struct.asFieldSet
import net.aquadc.persistence.struct.build
import net.aquadc.persistence.struct.plus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test


class PartialsTest {

    @Test(expected = NoSuchElementException::class)
    fun absent() {
        SomeSchema.buildPartial { }[SomeSchema.A]
    }

    @Test fun partial() {
        val p = SomeSchema.buildPartial {
            it[A] = "123"
            it[B] = 99
        }
        assertEquals("123", p[SomeSchema.A])
        assertEquals(99, p[SomeSchema.B])
        assertEquals(SomeSchema.A + SomeSchema.B, p.fields)
    }

    @Test fun full() {
        val p = SomeSchema.buildPartial {
            it[A] = "123"
            it[B] = 99
            it[C] = 100500L
        }
        assertEquals("123", p[SomeSchema.A])
        assertEquals(99, p[SomeSchema.B])
        assertEquals(100500L, p[SomeSchema.C])
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
        assertEquals(taken[SomeSchema.A], "")
        assertEquals(taken[SomeSchema.C], 1L)
    }

    @Test fun `take all`() {
        val full = SomeSchema.build {
            it[A] = ""
            it[B] = 1
            it[C] = 1L
        }

        assertEquals(full, full.take(SomeSchema.allFieldSet()))
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
