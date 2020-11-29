package net.aquadc.properties

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Bugs {

    @Test fun `unsubscribed and resubscribed during notification uns`() =
        `unsubscribed and resubscribed during notification`(false)
    @Test fun `unsubscribed and resubscribed during notification conc`() =
        `unsubscribed and resubscribed during notification`(true)
    fun `unsubscribed and resubscribed during notification`(concurrent: Boolean) {
        val wantProp = propertyOf(false, concurrent)
        val canProp = propertyOf(false, concurrent)
        val wantCanProp = wantProp.zipWith(canProp)
        val willProp = wantCanProp.map { (want, _) ->
            want
        }

        var first: ChangeListener<Boolean>? = null
        var want = false
        first = { _, will ->
            if (will) {
                willProp.removeChangeListener(first!!)

                val someListener = object : (Pair<Boolean, Boolean>, Pair<Boolean, Boolean>) -> Unit {
                    override fun invoke(p1: Pair<Boolean, Boolean>, p2: Pair<Boolean, Boolean>) {
                        want = p2.first
                    }
                    override fun toString(): String =
                        "someListener"
                }
                wantCanProp.addUnconfinedChangeListener(someListener)
                someListener.invoke(false to false, wantCanProp.value)
            }
        }
        willProp.addUnconfinedChangeListener(first)

        wantProp.value = true
        assertTrue(want)

        wantProp.value = false
        assertFalse(want)

        wantProp.value = true
        assertTrue(want)
    }

}
