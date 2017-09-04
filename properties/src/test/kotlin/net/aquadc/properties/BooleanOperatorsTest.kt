package net.aquadc.properties

import org.junit.Assert.assertEquals
import org.junit.Test

class BooleanOperatorsTest {

    @Test fun notTest() {
        val prop = mutablePropertyOf(true)
        val notProp = prop.not()
        assertEquals(false, notProp.value)

        prop.value = false
        assertEquals(true, notProp.value)
    }

    @Test fun andTest() {
        val p0 = mutablePropertyOf(true)
        val p1 = mutablePropertyOf(true)

        val and = p0 and p1
        assertEquals(true, and.value)

        p0.value = false
        assertEquals(false, and.value)

        p1.value = false
        assertEquals(false, and.value)

        p0.value = true
        assertEquals(false, and.value)

        p1.value = true
        assertEquals(true, and.value)
    }

    @Test fun orTest() {
        val p0 = mutablePropertyOf(true)
        val p1 = mutablePropertyOf(true)

        val and = p0 or p1
        assertEquals(true, and.value)

        p0.value = false
        assertEquals(true, and.value)

        p1.value = false
        assertEquals(false, and.value)

        p0.value = true
        assertEquals(true, and.value)

        p1.value = true
        assertEquals(true, and.value)
    }

}