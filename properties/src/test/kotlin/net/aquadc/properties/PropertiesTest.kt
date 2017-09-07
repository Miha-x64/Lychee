package net.aquadc.properties

import junit.framework.TestCase.assertEquals
import org.junit.Test

class PropertiesTest {

    @Test fun immutableProps() {
        val prop by immutablePropertyOf("hello")
        assertEquals("hello", prop)
    }

    @Test fun concMutableProps() = mutableProps(true)
    @Test fun unsMutableProps() = mutableProps(false)
    private fun mutableProps(concurrent: Boolean) {
        val prop = mutablePropertyOf("hello", concurrent)
        assertEquals("hello", prop.value)

        var old: String? = null
        var new: String? = null
        val listener: (String, String) -> Unit = { o, n -> old = o; new = n }
        prop.addChangeListener(listener)

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
        val prop = mutablePropertyOf(1, concurrent)
        val mapped = prop.map { 10 * it }
        assertEquals(10, mapped.value)

        prop.value = 5
        assertEquals(50, mapped.value)

        var old = -1
        var new = -1
        mapped.addChangeListener { o, n -> old = o; new = n }

        prop.value = -2
        assertEquals(50, old)
        assertEquals(-20, new)
    }

    @Test fun concBiMappedProperty() = biMappedProperty(true)
    @Test fun unsBiMappedProperty() = biMappedProperty(false)
    private fun biMappedProperty(concurrent: Boolean) {
        val prop0 = mutablePropertyOf("a", concurrent)
        val prop1 = mutablePropertyOf("b", concurrent)
        val biMapped = prop0.mapWith(prop1) { a, b -> "$a $b" }
        assertEquals("a b", biMapped.value)

        prop0.value = "b"
        assertEquals("b b", biMapped.value)

        var old: String? = null
        var new: String? = null
        biMapped.addChangeListener { o, n -> old = o; new = n }

        prop1.value = "c"
        assertEquals("b c", biMapped.value)
        assertEquals("b b", old)
        assertEquals("b c", new)
    }

}
