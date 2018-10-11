package net.aquadc.properties

import net.aquadc.properties.function.isBlank
import net.aquadc.properties.function.isEmptyCharSequence
import net.aquadc.properties.function.isNonEmptyCharSequence
import net.aquadc.properties.function.isNotBlank
import org.junit.Assert.*
import org.junit.Test


class CharSeqExts {

    @Test fun length() {
        val prop = propertyOf("")
        val len by prop.length
        assertEquals(0, len)

        prop.value = " "
        assertEquals(1, len)

        prop.value = "four"
        assertEquals(4, len)
    }

    @Test fun empty() {
        val prop = propertyOf("")

        val empty by prop.map(isEmptyCharSequence())
        val notEmpty by prop.map(isNonEmptyCharSequence())
        val blank by prop.map(isBlank())
        val notBlank by prop.map(isNotBlank())

        assertTrue(empty)
        assertFalse(notEmpty)
        assertTrue(blank)
        assertFalse(notBlank)

        prop.value = " "

        assertFalse(empty)
        assertTrue(notEmpty)
        assertTrue(blank)
        assertFalse(notBlank)

        prop.value = "x"

        assertFalse(empty)
        assertTrue(notEmpty)
        assertFalse(blank)
        assertTrue(notBlank)
    }

    @Test fun trimmed() {
        val prop = propertyOf("")
        val trimmed by prop.trimmed

        assertEquals("", trimmed)

        prop.value = " "
        assertEquals("", trimmed)

        prop.value = " x"
        assertEquals("x", trimmed)

        prop.value = "x "
        assertEquals("x", trimmed)

        prop.value = " x "
        assertEquals("x", trimmed)
    }

}
