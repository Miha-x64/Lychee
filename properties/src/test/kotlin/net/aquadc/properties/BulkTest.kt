package net.aquadc.properties

import org.junit.Assert.*
import org.junit.Test

class BulkTest {

    @Test fun concMapValueList() = mapValueList(true)
    @Test fun unsMapValueList() = mapValueList(false)
    private fun mapValueList(concurrent: Boolean) {
        assertEquals("hello", listOf<Property<Nothing>>().mapValueList { "hello" }.value)

        val prop0 = mutablePropertyOf(0, concurrent)
        val prop1 = mutablePropertyOf(1, concurrent)
        val prop2 = mutablePropertyOf(2, concurrent)
        val sum = listOf(prop0, prop1, prop2).mapValueList { it.fold(0) { a, b -> a + b } }
        assertEquals(3, sum.value)

        prop0.value = 5
        assertEquals(8, sum.value)
    }

    @Test fun concFoldValues() = foldValues(true)
    @Test fun unsFoldValues() = foldValues(false)
    private fun foldValues(concurrent: Boolean) {
        val prop0 = mutablePropertyOf(0, concurrent)
        val prop1 = mutablePropertyOf(1, concurrent)
        val prop2 = mutablePropertyOf(2, concurrent)
        val sum = listOf(prop0, prop1, prop2).foldValues(0) { a, b -> a + b }
        assertEquals(3, sum.value)

        prop0.value = 5
        assertEquals(8, sum.value)
    }

    @Test fun concFirstValueOrNull() = firstValueOrNull(true)
    @Test fun unsFirstValueOrNull() = firstValueOrNull(false)
    private fun firstValueOrNull(concurrent: Boolean) {
        val prop0 = mutablePropertyOf(0, concurrent)
        val prop1 = mutablePropertyOf(1, concurrent)
        val prop2 = mutablePropertyOf(2, concurrent)

        val firstOrNull = listOf(prop0, prop1, prop2).firstValueOrNull { it > 0 }
        assertEquals(1, firstOrNull.value)

        prop1.value = -3
        assertEquals(2, firstOrNull.value)

        prop2.value = 0
        assertEquals(null, firstOrNull.value)
    }

    @Test fun concFilterValues() = filterValues(true)
    @Test fun unsFilterValues() = filterValues(false)
    private fun filterValues(concurrent: Boolean) {
        val prop0 = mutablePropertyOf(0, concurrent)
        val prop1 = mutablePropertyOf(1, concurrent)
        val prop2 = mutablePropertyOf(2, concurrent)

        val filtered = listOf(prop0, prop1, prop2).filterValues { it > 0 }
        assertEquals(listOf(1, 2), filtered.value)

        prop1.value = 0
        assertEquals(listOf(2), filtered.value)

        prop2.value = -1
        assertEquals(emptyList<Int>(), filtered.value)
    }

    @Test fun concContainsValue() = containsValue(true)
    @Test fun unsContainsValue() = containsValue(false)
    private fun containsValue(concurrent: Boolean) {
        val prop0 = mutablePropertyOf(0, concurrent)
        val prop1 = mutablePropertyOf(1, concurrent)
        val prop2 = mutablePropertyOf(1, concurrent)

        val contains = listOf(prop0, prop1, prop2).containsValue(1)
        assertTrue(contains.value)

        prop1.value = 0
        assertTrue(contains.value)

        prop2.value = 0
        assertFalse(contains.value)
    }

    @Test fun concContainsAllValues() = containsAllValues(true)
    @Test fun unsContainsAllValues() = containsAllValues(false)
    private fun containsAllValues(concurrent: Boolean) {
        val prop0 = mutablePropertyOf(0, concurrent)
        val prop1 = mutablePropertyOf(1, concurrent)
        val prop2 = mutablePropertyOf(1, concurrent)

        val contains = listOf(prop0, prop1, prop2).containsAllValues(listOf(0, 1))
        assertTrue(contains.value)

        prop1.value = 0
        assertTrue(contains.value)

        prop2.value = 0
        assertFalse(contains.value)
    }

    @Test fun concAllAndAnyValues() = allAndAnyValues(true)
    @Test fun unsAllAndAnyValues() = allAndAnyValues(false)
    private fun allAndAnyValues(concurrent: Boolean) {
        val prop0 = mutablePropertyOf(0, concurrent)
        val prop1 = mutablePropertyOf(1, concurrent)
        val prop2 = mutablePropertyOf(2, concurrent)

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

    @Test fun concSubscription() = subscription(true)
    @Test fun unsSubscription() = subscription(false)
    private fun subscription(concurrent: Boolean) {
        val prop0 = mutablePropertyOf(0, concurrent)
        val prop1 = mutablePropertyOf(1, concurrent)
        val prop2 = mutablePropertyOf(2, concurrent)
        val sum = listOf(prop0, prop1, prop2).foldValues(0) { acc, value -> acc + value }

        assertEquals(3, sum.value)

        prop0.value = 10
        assertEquals(13, sum.value)
    }

}
