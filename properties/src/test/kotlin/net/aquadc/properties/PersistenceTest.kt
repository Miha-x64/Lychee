package net.aquadc.properties

import net.aquadc.properties.persistence.*
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.*
import net.aquadc.properties.unsynchronizedMutablePropertyOf as ump


class PersistenceTest : PersistableProperties {

    private val bo = ump(true)
    private val by = ump(0xF6.toByte())
    private val it = ump(2_000_000)
    private val lo = ump(200_000_000_000)
    private val fl = ump(100500.9f)
    private val dl = ump(99998888777666554.3)

    private val bya = ump(byteArrayOf(1, 2, 3, 4, 5))
    private val cha = ump(charArrayOf('K', 'o', 't', 'l', 'i', 'n', ' ', 'i', 's', ' ', 'a', 'w', 'e', 's', 'o', 'm', 'e'))
    private val ita = ump(intArrayOf(Int.MIN_VALUE, -1, 0, 1, Int.MAX_VALUE))
    private val loa = ump(longArrayOf(Long.MIN_VALUE, -1, 0, 1, Long.MAX_VALUE))
    private val fla = ump(floatArrayOf(-1.7f, 0f, 1.2f, Float.MIN_VALUE, Float.MAX_VALUE, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, Float.NaN))
    private val dla = ump(doubleArrayOf(-1.7, 0.0, 1.2, Double.MIN_VALUE, Double.MAX_VALUE, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NaN))

    private val str = ump("hello")
    private val stl = ump(listOf("zzz", "ff"))

    private val en = ump(Thread.State.BLOCKED)
    private val ens = ump(EnumSet.of(Thread.State.NEW, Thread.State.RUNNABLE))

    private fun moveToState1() {
        bo.value = true
        by.value = 0xF6.toByte()
        it.value = 2_000_000
        lo.value = 200_000_000_000
        fl.value = 100500.9f
        dl.value = 99998888777666554.3

        bya.value = byteArrayOf(1, 2, 3, 4, 5)
        cha.value = charArrayOf('K', 'o', 't', 'l', 'i', 'n', ' ', 'i', 's', ' ', 'a', 'w', 'e', 's', 'o', 'm', 'e')
        ita.value = intArrayOf(Int.MIN_VALUE, -1, 0, 1, Int.MAX_VALUE)
        loa.value = longArrayOf(Long.MIN_VALUE, -1, 0, 1, Long.MAX_VALUE)
        fla.value = floatArrayOf(-1.7f, 0f, 1.2f, Float.MIN_VALUE, Float.MAX_VALUE, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, Float.NaN)
        dla.value = doubleArrayOf(-1.7, 0.0, 1.2, Double.MIN_VALUE, Double.MAX_VALUE, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NaN)

        str.value = "hello"
        stl.value = listOf("zzz", "ff")

        en.value = Thread.State.BLOCKED
        ens.value = EnumSet.of(Thread.State.NEW, Thread.State.RUNNABLE)
    }

    private fun assertInState1() {
        assertTrue(bo.value)
        assertEquals(0xF6.toByte(), by.value)
        assertEquals(2_000_000, it.value)
        assertEquals(200_000_000_000, lo.value)
        assertEquals(100500.9f, fl.value)
        assertEquals(99998888777666554.3, dl.value, 0.0)
        assertArrayEquals(byteArrayOf(1, 2, 3, 4, 5), bya.value)
        assertArrayEquals(charArrayOf('K', 'o', 't', 'l', 'i', 'n', ' ', 'i', 's', ' ', 'a', 'w', 'e', 's', 'o', 'm', 'e'), cha.value)
        assertArrayEquals(intArrayOf(Int.MIN_VALUE, -1, 0, 1, Int.MAX_VALUE), ita.value)
        assertArrayEquals(longArrayOf(Long.MIN_VALUE, -1, 0, 1, Long.MAX_VALUE), loa.value)
        assertArrayEquals(floatArrayOf(-1.7f, 0f, 1.2f, Float.MIN_VALUE, Float.MAX_VALUE, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, Float.NaN), fla.value, 0f)
        assertArrayEquals(doubleArrayOf(-1.7, 0.0, 1.2, Double.MIN_VALUE, Double.MAX_VALUE, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NaN), dla.value, 0.0)
        assertEquals("hello", str.value)
        assertEquals(listOf("zzz", "ff"), stl.value)
        assertEquals(Thread.State.BLOCKED, en.value)
        assertEquals(setOf(Thread.State.NEW, Thread.State.RUNNABLE), ens.value)
    }

    private fun moveToState2() {
        bo.clear()
        by.value = 4
        it.value = -2_147_000_111
        lo.value = Long.MIN_VALUE
        fl.value = -987.65432f
        dl.value = -123456789.00004
        bya.value = byteArrayOf(0x7F, 0xFF.toByte())
        cha.value = charArrayOf('j', 'v', 'm')
        ita.value = intArrayOf(1)
        loa.value = longArrayOf(0)
        fla.value = floatArrayOf(1.1f)
        dla.value = doubleArrayOf(-9.6)
        str.value = "goodbye"
        stl.value = listOf("wow")
        en.value = Thread.State.NEW
        ens.value = EnumSet.of(Thread.State.WAITING)
    }

    private fun assertInState2() {
        assertFalse(bo.value)
        assertEquals(4.toByte(), by.value)
        assertEquals(-2_147_000_111, it.value)
        assertEquals(Long.MIN_VALUE, lo.value)
        assertEquals(-987.65432f, fl.value)
        assertEquals(-123456789.00004, dl.value, 0.0)
        assertArrayEquals(byteArrayOf(0x7F, 0xFF.toByte()), bya.value)
        assertArrayEquals(charArrayOf('j', 'v', 'm'), cha.value)
        assertArrayEquals(intArrayOf(1), ita.value)
        assertArrayEquals(longArrayOf(0), loa.value)
        assertArrayEquals(floatArrayOf(1.1f), fla.value, 0f)
        assertArrayEquals(doubleArrayOf(-9.6), dla.value, 0.0)
        assertEquals("goodbye", str.value)
        assertEquals(listOf("wow"), stl.value)
        assertEquals(Thread.State.NEW, en.value)
        assertEquals(setOf(Thread.State.WAITING), ens.value)
    }

    @Test fun propertyIoTest() {
        moveToState1()
        val initial = ByteArrayPropertiesMemento(this)

        moveToState2()
        val changed = ByteArrayPropertiesMemento(this)

        initial.restoreTo(this)
        assertInState1()

        changed.restoreTo(this)
        assertInState2()
    }

    @Test fun propBufferTest() {
        moveToState1()
        PropertyBuffer.borrow { pd, pb ->
            saveOrRestore(pd)
            pb.produceThen()

            moveToState2()
            saveOrRestore(pd)
            pb.consumeThen()
        }
        assertInState1()

        moveToState2()
        PropertyBuffer.borrow { pd, pb ->
            saveOrRestore(pd)
            pb.produceThen()

            moveToState1()
            saveOrRestore(pd)
            pb.consumeThen()
        }
        assertInState2()
    }

    @Test fun baPropMemento() {
        moveToState1()
        val memento = ByteArrayPropertiesMemento(this)
        val baos = ByteArrayOutputStream()
        val oos = ObjectOutputStream(baos)
        oos.writeObject(memento)

        val ois = ObjectInputStream(ByteArrayInputStream(baos.toByteArray()))
        val restored = ois.readObject()

        assertNotSame(memento, restored)
        assertArrayEquals(memento.copyValue(), (restored as ByteArrayPropertiesMemento).copyValue())
    }

    override fun saveOrRestore(d: PropertyIo) {
        d x bo; d x by; d x it; d x lo; d x fl; d x dl
        d x bya; d x cha; d x ita; d x loa; d x fla; d x dla
        d x str; d x stl
        d x en; d x ens
    }

}
