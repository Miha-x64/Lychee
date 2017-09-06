package net.aquadc.properties

import org.junit.Assert.*
import org.junit.Test

class BulkTest {

    @Test fun mapValueList() {
        assertEquals("hello", listOf<Property<Nothing>>().mapValueList { "hello" }.value)

        val prop0 = mutablePropertyOf(0)
        val prop1 = mutablePropertyOf(1)
        val prop2 = mutablePropertyOf(2)
        val sum = listOf(prop0, prop1, prop2).mapValueList { it.fold(0) { a, b -> a + b } }
        assertEquals(3, sum.value)

        prop0.value = 5
        assertEquals(8, sum.value)
    }

    @Test fun foldValues() {
        val prop0 = mutablePropertyOf(0)
        val prop1 = mutablePropertyOf(1)
        val prop2 = mutablePropertyOf(2)
        val sum = listOf(prop0, prop1, prop2).foldValues(0) { a, b -> a + b }
        assertEquals(3, sum.value)

        prop0.value = 5
        assertEquals(8, sum.value)
    }

    @Test fun firstValueOrNull() {
        val prop0 = mutablePropertyOf(0)
        val prop1 = mutablePropertyOf(1)
        val prop2 = mutablePropertyOf(2)

        val firstOrNull = listOf(prop0, prop1, prop2).firstValueOrNull { it > 0 }
        assertEquals(1, firstOrNull.value)

        prop1.value = -3
        assertEquals(2, firstOrNull.value)

        prop2.value = 0
        assertEquals(null, firstOrNull.value)
    }

    @Test fun filterValues() {
        val prop0 = mutablePropertyOf(0)
        val prop1 = mutablePropertyOf(1)
        val prop2 = mutablePropertyOf(2)

        val filtered = listOf(prop0, prop1, prop2).filterValues { it > 0 }
        assertEquals(listOf(1, 2), filtered.value)

        prop1.value = 0
        assertEquals(listOf(2), filtered.value)

        prop2.value = -1
        assertEquals(emptyList<Int>(), filtered.value)
    }

    @Test fun containsValue() {
        val prop0 = mutablePropertyOf(0)
        val prop1 = mutablePropertyOf(1)
        val prop2 = mutablePropertyOf(1)

        val contains = listOf(prop0, prop1, prop2).containsValue(1)
        assertTrue(contains.value)

        prop1.value = 0
        assertTrue(contains.value)

        prop2.value = 0
        assertFalse(contains.value)
    }

    @Test fun containsAllValues() {
        val prop0 = mutablePropertyOf(0)
        val prop1 = mutablePropertyOf(1)
        val prop2 = mutablePropertyOf(1)

        val contains = listOf(prop0, prop1, prop2).containsAllValues(listOf(0, 1))
        assertTrue(contains.value)

        prop1.value = 0
        assertTrue(contains.value)

        prop2.value = 0
        assertFalse(contains.value)
    }

    @Test fun allAndAnyValues() {
        val prop0 = mutablePropertyOf(0)
        val prop1 = mutablePropertyOf(1)
        val prop2 = mutablePropertyOf(2)

        val all = listOf(prop0, prop1, prop2).allValues { it > 0 }
        val any = listOf(prop0, prop1, prop2).anyValue { it > 0 }

        assertFalse(all.value)
        assertTrue(any.value)

        prop0.value = 5
        assertTrue(all.value)
        assertTrue(any.value)

        prop2.value = -1
        assertFalse(all.value)
        assertTrue(any.value)

        prop1.value = -2
        assertFalse(all.value)
        assertTrue(any.value)

        prop0.value = -3
        assertFalse(all.value)
        assertFalse(any.value)
    }

}
