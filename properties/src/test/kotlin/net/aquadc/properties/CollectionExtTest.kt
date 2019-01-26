package net.aquadc.properties

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test


class CollectionExtTest {

    @Test fun withMapping1() {
        val prop = propertyOf(mapOf<String, Int>())
        prop += "one" to 1
        assertEquals(mapOf("one" to 1), prop.value)
        assertSame(mapOf("one" to 1).javaClass, prop.value.javaClass)
    }

    @Test fun withMapping2() {
        val prop = propertyOf(mapOf<String, Number>("one" to 1))

        prop += "two" to 2
        assertEquals(mapOf("one" to 1, "two" to 2), prop.value)

        prop += "two" to 2.0
        assertEquals(mapOf("one" to 1, "two" to 2.0), prop.value)
    }

    @Test fun withoutMapping0() {
        val prop = propertyOf(mapOf<String, Int>())

        prop -= "whatever"
        assertSame(emptyMap<String, Int>(), prop.value)
    }

    @Test fun withoutMapping1() {
        val initial = mapOf("one" to 1)
        val prop = propertyOf(initial)

        prop -= "whatever"
        assertSame(initial, prop.value)

        prop -= "one"
        assertSame(emptyMap<String, Int>(), prop.value)
    }

    @Test fun withoutMapping2() {
        val initial = mapOf("one" to 1, "two" to 2)
        val prop = propertyOf(initial)

        prop -= "whatever"
        assertSame(initial, prop.value)

        prop -= "two"
        assertEquals(mapOf("one" to 1), prop.value)
        assertSame(mapOf("one" to 1).javaClass, prop.value.javaClass)
    }

    @Test fun listWithElement0() {
        val prop = propertyOf(listOf<String>())
        prop += "something"
        assertEquals(listOf("something"), prop.value)
        assertSame(listOf("whatever").javaClass, prop.value.javaClass)
    }

    @Test fun listWithElement1() {
        val prop = propertyOf(listOf("something"))
        prop += "anything"
        assertEquals(listOf("something", "anything"), prop.value)
    }

    @Test fun setWithElement0() {
        val prop = propertyOf(setOf<String>())
        prop += "something"
        assertEquals(setOf("something"), prop.value)
        assertSame(setOf("whatever").javaClass, prop.value.javaClass)
    }

    @Test fun setWithElement1() {
        val initial = setOf("something")
        val prop = propertyOf(initial)

        prop += "something"
        assertSame(initial, prop.value)

        prop += "anything"
        assertEquals(setOf("something", "anything"), prop.value)
    }

    @Test fun setWithoutElement0() {
        val prop = propertyOf(setOf<String>())
        prop -= "whatever"
        assertSame(setOf<String>(), prop.value)
    }

    @Test fun setWithoutElement1() {
        val initial = setOf("something")
        val prop = propertyOf(initial)

        prop -= "whatever"
        assertSame(initial, prop.value)

        prop -= "something"
        assertSame(setOf<String>(), prop.value)
    }

    @Test fun setWithoutElement2() {
        val initial = setOf("something", "anything")
        val prop = propertyOf(initial)

        prop -= "whatever"
        assertSame(initial, prop.value)

        prop -= "something"
        assertEquals(setOf("anything"), prop.value)
        assertSame(setOf("anything").javaClass, prop.value.javaClass)
    }

}
