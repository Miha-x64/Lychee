package net.aquadc.persistence.extended

import net.aquadc.persistence.extended.inctemental.Incremental3
import net.aquadc.persistence.extended.inctemental.emptyIncremental
import net.aquadc.persistence.extended.inctemental.mapFold
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Test


class IncrementalTest {

    @Test fun saturation() {
        var inc = emptyIncremental<Incremental3<String, String, String>>()
        assertNotEquals(inc, inc.saturatingFill().also { inc = it }) // (    ) + a => (a)
        assertNotEquals(inc, inc.saturatingFill().also { inc = it }) // (a   ) + b => (a, b)
        assertNotEquals(inc, inc.saturatingFill().also { inc = it }) // (a, b) + c => (a, b, c)
        assertEquals(inc, inc.saturatingFill())
        assertSame(inc, inc.saturatingFill())
    }

    private fun Incremental3<String, String, String>.saturatingFill(): Incremental3<String, String, String> = mapFold(
        { next -> next("a") },
        { _, next -> next("b") },
        { _, _, next -> next("c") },
        { _, _, _ -> this }
    )

}
