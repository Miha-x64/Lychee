package net.aquadc.persistence

import net.aquadc.persistence.type.*
import net.aquadc.properties.function.Enumz
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.*


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

    @Test fun enumSetAsLong() {
        val es = enumSet<Thread.State>(long, Enumz.Ordinal)
        assertEquals(0L, es.encode(EnumSet.noneOf(Thread.State::class.java)))
        assertEquals(1L, es.encode(EnumSet.of(Thread.State.NEW)))
        assertEquals(2L, es.encode(EnumSet.of(Thread.State.RUNNABLE)))
        assertEquals(4L, es.encode(EnumSet.of(Thread.State.BLOCKED)))
        assertEquals(6L, es.encode(EnumSet.of(Thread.State.RUNNABLE, Thread.State.BLOCKED)))
        assertEquals(7L, es.encode(EnumSet.of(Thread.State.NEW, Thread.State.RUNNABLE, Thread.State.BLOCKED)))

        assertEquals(EnumSet.of(Thread.State.NEW, Thread.State.RUNNABLE, Thread.State.BLOCKED), es.decode(7L))
        assertEquals(EnumSet.of(Thread.State.RUNNABLE, Thread.State.BLOCKED), es.decode(6L))
        assertEquals(EnumSet.of(Thread.State.BLOCKED), es.decode(4L))
        assertEquals(EnumSet.of(Thread.State.RUNNABLE), es.decode(2L))
        assertEquals(EnumSet.of(Thread.State.NEW), es.decode(1L))
        assertEquals(emptySet<Nothing>(), es.decode(0L))
    }

    @Test(expected = NoSuchElementException::class) fun noEnumConst() {
        enum(Thread.State.values(), string, Enumz.Name).decode("NOPE")
    }

}
