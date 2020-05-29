package net.aquadc.persistence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReallyX {

    val a = listOf(intArrayOf(1, 2, 3), "456")
    val b = listOf(intArrayOf(1, 2, 3), StringBuilder("456"))

    @Test fun eq() =
        assertTrue(reallyEqual(a, b))

    @Test fun hc() =
        assertEquals(a.realHashCode(), b.realHashCode())

    @Test fun aToS() =
        assertEquals(
            "[[1, 2, 3], 456]",
            a.realToString()
        )

    @Test fun bToS() =
        assertEquals(
            "[[1, 2, 3], 456]",
            b.realToString()
        )

}
