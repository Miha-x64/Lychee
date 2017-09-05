package net.aquadc.properties

import org.junit.Assert.assertEquals
import org.junit.Test

class BooleanOperatorsTest {

    @Test fun notTest() {
        val prop = mutablePropertyOf(true)
        val notProp by prop.not()
        assertEquals(false, notProp)

        prop.value = false
        assertEquals(true, notProp)
    }

    @Test fun andTest() {
        val p0 = mutablePropertyOf(true)
        val p1 = mutablePropertyOf(true)

        val and by p0 and p1
        assertEquals(true, and)

        p0.value = false
        assertEquals(false, and)

        p1.value = false
        assertEquals(false, and)

        p0.value = true
        assertEquals(false, and)

        p1.value = true
        assertEquals(true, and)
    }

    @Test fun orTest() {
        val p0 = mutablePropertyOf(true)
        val p1 = mutablePropertyOf(true)

        val and by p0 or p1
        assertEquals(true, and)

        p0.value = false
        assertEquals(true, and)

        p1.value = false
        assertEquals(false, and)

        p0.value = true
        assertEquals(true, and)

        p1.value = true
        assertEquals(true, and)
    }

    @Test fun xorTest() {
        val p0 = mutablePropertyOf(true)
        val p1 = mutablePropertyOf(true)

        val and by p0 xor p1
        assertEquals(false, and)

        p0.value = false
        assertEquals(true, and)

        p1.value = false
        assertEquals(false, and)

        p0.value = true
        assertEquals(true, and)

        p1.value = true
        assertEquals(false, and)
    }

}