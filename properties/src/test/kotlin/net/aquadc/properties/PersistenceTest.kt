package net.aquadc.properties

import net.aquadc.properties.persistence.PropertyIo
import net.aquadc.properties.persistence.memento.ByteArrayPropertiesMemento
import net.aquadc.properties.persistence.memento.PersistableProperties
import net.aquadc.properties.persistence.memento.restoreTo
import net.aquadc.properties.persistence.x
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import net.aquadc.properties.propertyOf as ump


class PersistenceTest : PersistableProperties {

    private val bo = ump(true)
    private val by = ump(0xF6.toByte())
    private val it = ump(2_000_000)
    private val lo = ump(200_000_000_000)
    private val fl = ump(100500.9f)
    private val dl = ump(99998888777666554.3)

    private val bya = ump(byteArrayOf(1, 2, 3, 4, 5))

    private val str = ump("hello")
    private val stl = ump(listOf("zzz", "ff"))

    private fun moveToState1() {
        bo.value = true
        by.value = 0xF6.toByte()
        it.value = 2_000_000
        lo.value = 200_000_000_000
        fl.value = 100500.9f
        dl.value = 99998888777666554.3

        bya.value = byteArrayOf(1, 2, 3, 4, 5)

        str.value = "hello"
        stl.value = listOf("zzz", "ff")
    }

    private fun assertInState1() {
        assertTrue(bo.value)
        assertEquals(0xF6.toByte(), by.value)
        assertEquals(2_000_000, it.value)
        assertEquals(200_000_000_000, lo.value)
        assertEquals(100500.9f, fl.value)
        assertEquals(99998888777666554.3, dl.value, 0.0)
        assertArrayEquals(byteArrayOf(1, 2, 3, 4, 5), bya.value)
        assertEquals("hello", str.value)
        assertEquals(listOf("zzz", "ff"), stl.value)
    }

    private fun moveToState2() {
        bo.clear()
        by.value = 4
        it.value = -2_147_000_111
        lo.value = Long.MIN_VALUE
        fl.value = -987.65432f
        dl.value = -123456789.00004
        bya.value = byteArrayOf(0x7F, 0xFF.toByte())
        str.value = "goodbye"
        stl.value = listOf("wow")
    }

    private fun assertInState2() {
        assertFalse(bo.value)
        assertEquals(4.toByte(), by.value)
        assertEquals(-2_147_000_111, it.value)
        assertEquals(Long.MIN_VALUE, lo.value)
        assertEquals(-987.65432f, fl.value)
        assertEquals(-123456789.00004, dl.value, 0.0)
        assertArrayEquals(byteArrayOf(0x7F, 0xFF.toByte()), bya.value)
        assertEquals("goodbye", str.value)
        assertEquals(listOf("wow"), stl.value)
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

    override fun saveOrRestore(io: PropertyIo) {
        io x bo; io x by; io x it; io x lo; io x fl; io x dl
        io x bya
        io x str; io x stl
    }

}
