package net.aquadc.persistence

import net.aquadc.persistence.type.*
import net.aquadc.properties.function.Enumz
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.*


class EnumType {

    @Test fun enumAsString() {
        val e = enum(Thread.State.values(), string, Enumz.Name)
        assertEquals("NEW", e.store(Thread.State.NEW))
        assertEquals("BLOCKED", e.store(Thread.State.BLOCKED))
        assertEquals(Thread.State.BLOCKED, e.load("BLOCKED"))
        assertEquals(Thread.State.NEW, e.load("NEW"))
    }

    @Test fun enumAsInt() {
        val e = enum(Thread.State.values(), i32, Enumz.Ordinal)
        assertEquals(0, e.store(Thread.State.NEW))
        assertEquals(2, e.store(Thread.State.BLOCKED))
        assertEquals(Thread.State.BLOCKED, e.load(2))
        assertEquals(Thread.State.NEW, e.load(0))
    }

    @Test fun enumSetAsLong() {
        val es = enumSet<Thread.State>(i64, Enumz.Ordinal)
        assertEquals(0L, es.store(EnumSet.noneOf(Thread.State::class.java)))
        assertEquals(1L, es.store(EnumSet.of(Thread.State.NEW)))
        assertEquals(2L, es.store(EnumSet.of(Thread.State.RUNNABLE)))
        assertEquals(4L, es.store(EnumSet.of(Thread.State.BLOCKED)))
        assertEquals(6L, es.store(EnumSet.of(Thread.State.RUNNABLE, Thread.State.BLOCKED)))
        assertEquals(7L, es.store(EnumSet.of(Thread.State.NEW, Thread.State.RUNNABLE, Thread.State.BLOCKED)))

        assertEquals(EnumSet.of(Thread.State.NEW, Thread.State.RUNNABLE, Thread.State.BLOCKED), es.load(7L))
        assertEquals(EnumSet.of(Thread.State.RUNNABLE, Thread.State.BLOCKED), es.load(6L))
        assertEquals(EnumSet.of(Thread.State.BLOCKED), es.load(4L))
        assertEquals(EnumSet.of(Thread.State.RUNNABLE), es.load(2L))
        assertEquals(EnumSet.of(Thread.State.NEW), es.load(1L))
        assertEquals(emptySet<Nothing>(), es.load(0L))
    }

    @Test(expected = NoSuchElementException::class) fun noEnumConst() {
        enum(Thread.State.values(), string, Enumz.Name).load("NOPE")
    }

}
