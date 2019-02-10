package net.aquadc.properties

import net.aquadc.properties.function.CharSequencez
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test


class CharSeqExts {

    @Test fun length() {
        val prop = propertyOf("")
        val len by prop.map(CharSequencez.Length)
        assertEquals(0, len)

        prop.value = " "
        assertEquals(1, len)

        prop.value = "four"
        assertEquals(4, len)
    }

    @Test fun empty() {
        val prop = propertyOf("")

        val empty by prop.map(CharSequencez.Empty)
        val notEmpty by prop.map(CharSequencez.NotEmpty)
        val blank by prop.map(CharSequencez.Blank)
        val notBlank by prop.map(CharSequencez.NotBlank)

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
        val trimmed by prop.map(CharSequencez.Trim)

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
