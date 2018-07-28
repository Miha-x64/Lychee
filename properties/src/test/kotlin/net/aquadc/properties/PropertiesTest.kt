package net.aquadc.properties

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class PropertiesTest {

    @Test fun immutableProps() {
        val prop = immutablePropertyOf("hello")
        assertEquals("hello", prop.value)
        assertTrue(prop.isConcurrent)
        assertFalse(prop.mayChange)

        val onChange = { _: String, _: String -> }
        assertEquals(Unit, prop.addUnconfinedChangeListener(onChange))
        assertEquals(Unit, prop.addChangeListener(onChange))
        assertEquals(Unit, prop.removeChangeListener(onChange))
        // they're no-op, just for coverage ;)
    }

    @Test fun concMutableProps() = mutableProps(true)
    @Test fun unsMutableProps() = mutableProps(false)
    private fun mutableProps(concurrent: Boolean) {
        val prop = propertyOf("hello", concurrent)
        assertEquals("hello", prop.value)

        var old: String? = null
        var new: String? = null
        val listener: (String, String) -> Unit = { o, n -> old = o; new = n }
        prop.addUnconfinedChangeListener(listener)

        prop.value = "world"
        assertEquals("hello", old!!)
        assertEquals("world", new!!)
        assertEquals("world", prop.value)

        prop.value = "fizz"
        assertEquals("world", old!!)
        assertEquals("fizz", new!!)
        assertEquals("fizz", prop.value)

        prop.removeChangeListener(listener)
        prop.value = "buzz"
        assertEquals("world", old!!) // unchanged
        assertEquals("fizz", new!!) // unchanged
        assertEquals("buzz", prop.value)
    }

    @Test fun concMappedProp() = mappedProp(true)
    @Test fun unsMappedProp() = mappedProp(false)
    private fun mappedProp(concurrent: Boolean) {
        val prop = propertyOf(1, concurrent)
        val mapped = prop.map { 10 * it }
        assertEquals(10, mapped.value)

        prop.value = 5
        assertEquals(50, mapped.value)

        var old = -1
        var new = -1
        mapped.addUnconfinedChangeListener { o, n -> old = o; new = n }

        prop.value = -2
        assertEquals(50, old)
        assertEquals(-20, new)
    }

    @Test fun concBiMappedProperty() = biMappedProperty(true)
    @Test fun unsBiMappedProperty() = biMappedProperty(false)
    private fun biMappedProperty(concurrent: Boolean) {
        val prop0 = propertyOf("a", concurrent)
        val prop1 = propertyOf("b", concurrent)
        val biMapped = prop0.mapWith(prop1) { a, b -> "$a $b" }
        assertEquals("a b", biMapped.value)

        prop0.value = "b"
        assertEquals("b b", biMapped.value)

        var old: String? = null
        var new: String? = null
        biMapped.addUnconfinedChangeListener { o, n -> old = o; new = n }

        prop1.value = "c"
        assertEquals("b c", biMapped.value)
        assertEquals("b b", old)
        assertEquals("b c", new)
    }

    @Test fun concBidiMapped() = bidiMapped(true)
    @Test fun unsBidiMapped() = bidiMapped(false)
    private fun bidiMapped(concurrent: Boolean) {
        val a = propertyOf(10, concurrent)

        val b = a.bind({ 10 * it }, { it / 10 })
        assertEquals(100, b.value)

        b.value = 10
        assertEquals(1, a.value)

        val c = b.bind({ it + 50 }, { it - 50})
        assertEquals(c.value, 60)

        c.value = 130
        assertEquals(80, b.value)
        assertEquals(8, a.value)
    }

}
