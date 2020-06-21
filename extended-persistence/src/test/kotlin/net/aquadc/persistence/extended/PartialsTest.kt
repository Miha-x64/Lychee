package net.aquadc.persistence.extended

import net.aquadc.persistence.struct.getOrThrow
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.struct.StructSnapshot
import net.aquadc.persistence.struct.asFieldSet
import net.aquadc.persistence.struct.invoke
import net.aquadc.persistence.struct.plus
import net.aquadc.persistence.type.nullable
import net.aquadc.persistence.type.string
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
        /*val p = SomeSchema.buildPartial {
            it[A] = "123"
            it[B] = 99
        }
        assertEquals("123", p.getOrThrow(SomeSchema.A))
        assertEquals(99, p.getOrThrow(SomeSchema.B))
        assertEquals(-1L, p.getOrDefault(SomeSchema.C, -1L))
        assertEquals(SomeSchema.A + SomeSchema.B, p.fields)*/
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
        assertEquals(SomeSchema.allFieldSet, p.fields)
    }

    @Test fun take() {
        /*val taken = SomeSchema {
            it[A] = ""
            it[B] = 1
            it[C] = 1L
        }.take(SomeSchema.A + SomeSchema.C)

        assertEquals(SomeSchema.A + SomeSchema.C, taken.fields)
        assertEquals("", taken.getOrThrow(SomeSchema.A))
        assertEquals(-1, taken.getOrElse(SomeSchema.B) { -1 })
        assertEquals(1L, taken.getOrThrow(SomeSchema.C))*/
    }

    @Test fun `take all`() {
        // upcast to box inline type…
        val full: Struct<SomeSchema> = SomeSchema {
            it[A] = ""
            it[B] = 1
            it[C] = 1L
        }

        // …otherwise we'd box it here twice and get different instances
        assertSame(full, full.take(SomeSchema.allFieldSet))
    }

    @Test fun `copy from`() {
        /*assertEquals(SomeSchema.buildPartial {
            it[A] = "a"
            it[B] = 1
        }, SomeSchema.buildPartial {
            it[A] = "a"
        }.copy {
            val changed = it.setFrom(SomeSchema.buildPartial {
                it[B] = 1
                it[C] = 1L
            }, A + B)
            assertEquals(B.asFieldSet(), changed)
        })*/
    }

    @Test fun `copy partially`() {
        /*val some = SomeSchema {
            it[A] = "some"
            it[B] = 2
            it[C] = 6L
        }

        assertEquals(
                SomeSchema.buildPartial {
                    it[C] = 6L
                },
                some.copy(SomeSchema.C.asFieldSet())
        )*/
    }

    @Test fun `create packed`() {
        /*assertEquals(SomeSchema.buildPartial {
            it[B] = 99
            it[C] = 100L
        }, partial(SomeSchema).load(SomeSchema.B + SomeSchema.C, arrayOf<Any>(99, 100L)))*/
    }

    @Test fun `create single`() {
        /*assertEquals(SomeSchema.buildPartial {
            it[B] = 99
        }, partial(SomeSchema).load(SomeSchema.B.asFieldSet(), 99))*/
    }

    object CoolSchema : Schema<CoolSchema>() {
        val Single = "single" let nullable(string)
    }

    @Test fun singleNull() {
        /*val partial = CoolSchema.buildPartial { it[Single] = null }
        assertEquals(partial, partial(CoolSchema).load(CoolSchema.Single.asFieldSet(), null))*/
    }

}
