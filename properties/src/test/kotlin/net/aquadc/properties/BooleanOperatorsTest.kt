package net.aquadc.properties

import org.junit.Assert.assertEquals
import org.junit.Test

class BooleanOperatorsTest {

    @Test fun concNot() = not(true)
    @Test fun unsNot() = not(false)
    private fun not(concurrent: Boolean) {
        val prop = propertyOf(true, concurrent)
        val notProp by prop.not()
        assertEquals(false, notProp)

        prop.value = false
        assertEquals(true, notProp)
    }

    @Test fun concAnd() = and(true)
    @Test fun unsAnd() = and(false)
    private fun and(concurrent: Boolean) {
        val p0 = propertyOf(true, concurrent)
        val p1 = propertyOf(true, concurrent)

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

    @Test fun concOr() = or(true)
    @Test fun unsOr() = or(false)
    private fun or(concurrent: Boolean) {
        val p0 = propertyOf(true, concurrent)
        val p1 = propertyOf(true, concurrent)

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

    @Test fun concXor() = xor(true)
    @Test fun unsXor() = xor(false)
    private fun xor(concurrent: Boolean) {
        val p0 = propertyOf(true, concurrent)
        val p1 = propertyOf(true, concurrent)

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
