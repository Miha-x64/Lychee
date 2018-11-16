package net.aquadc.persistence

import net.aquadc.persistence.type.enum
import net.aquadc.persistence.type.int
import net.aquadc.persistence.type.string
import net.aquadc.properties.function.Enumz
import org.junit.Assert.assertEquals
import org.junit.Test


class EnumType {

    @Test fun enumAsString() {
        val e = enum(Thread.State.values(), string, Enumz.Name)
        assertEquals("NEW", e.encode(Thread.State.NEW))
        assertEquals("BLOCKED", e.encode(Thread.State.BLOCKED))
        assertEquals(Thread.State.BLOCKED, e.decode("BLOCKED"))
        assertEquals(Thread.State.NEW, e.decode("NEW"))
    }

    @Test fun enumAsInt() {
        val e = enum(Thread.State.values(), int, Enumz.Ordinal)
        assertEquals(0, e.encode(Thread.State.NEW))
        assertEquals(2, e.encode(Thread.State.BLOCKED))
        assertEquals(Thread.State.BLOCKED, e.decode(2))
        assertEquals(Thread.State.NEW, e.decode(0))
    }

    @Test(expected = NoSuchElementException::class) fun noEnumConst() {
        enum(Thread.State.values(), string, Enumz.Name).decode("NOPE")
    }

}
