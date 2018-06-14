package net.aquadc.properties

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.TimeUnit


class SubscriptionTest {

    @Test fun concSubscribe() = subscribe(true)
    @Test fun unsSubscribe() = subscribe(false)

    private fun subscribe(conc: Boolean) {
        val prop = propertyOf(false, conc)

        var l2Called = false

        val l2 = { _: Boolean, _: Boolean ->
            l2Called = true
        }

        val l1 = { _: Boolean, _: Boolean ->
            prop.addUnconfinedChangeListener(l2)
            Unit
        }

        prop.addUnconfinedChangeListener(l1)

        prop.value = false

        assertTrue(l2Called)

        l2Called = false
        prop.removeChangeListener(l1)

        assertTrue(prop.casValue(false, true))
        assertFalse(prop.casValue(false, true))
        assertTrue(l2Called)

        l2Called = false
        prop.removeChangeListener(l2)
        assertFalse(l2Called)
    }

    @Test fun concUnsubscribe() = unsubscribe(true)
    @Test fun unsUnsubscribe() = unsubscribe(false)

    private fun unsubscribe(conc: Boolean) {
        val prop = propertyOf(false, conc)

        var l1Called = 0
        var l2Called = 0
        var l3Called = 0

        val l1 = { _: Boolean, _: Boolean ->
            l1Called++
            Unit
        }

        val l3 = { _: Boolean, _: Boolean ->
            l3Called++
            Unit
        }

        val l2 = object : ChangeListener<Boolean> {
            override fun invoke(old: Boolean, new: Boolean) {
                l2Called++
                prop.removeChangeListener(this)
                prop.removeChangeListener(l3)
            }
        }

        prop.addUnconfinedChangeListener(l1)
        prop.addUnconfinedChangeListener(l2)
        prop.addUnconfinedChangeListener(l3)

        prop.value = false // l1Called++, l2Called++, remove l2 & l3
        prop.value = false // l1Called++

        assertEquals(2, l1Called)
        assertEquals(1, l2Called)
        assertEquals(0, l3Called)
    }

    @Test fun concUpdateInside() = updateInside(true)
    @Test fun unsUpdateInside() = updateInside(false)

    private fun updateInside(conc: Boolean) {
        val prop = propertyOf(0, conc)

        var v1 = 0
        var v2 = 0

        val l2 = { _: Int, new: Int ->
            v2 = new
        }

        val l1 = { _: Int, new: Int ->
            v1 = new
            if (new != 10) prop.value = 10
        }

        prop.addUnconfinedChangeListener(l1)
        prop.addUnconfinedChangeListener(l2)

        prop.value = 1

        assertEquals(10, v1)
        assertEquals(10, v2)
    }

    @Test fun onEach() {
        val prop = propertyOf("")
        val values = ArrayList<String>()
        prop.onEach { values.add(it) }
        prop.value = "new"
        assertEquals(listOf("", "new"), values)
    }

    @Test fun clearEach() {
        val prop = propertyOf(true)
        var called = 0
        prop.clearEachAnd { called++ }
        assertEquals(1, called)
        assertFalse(prop.value)

        prop.set()
        assertEquals(2, called)
        assertFalse(prop.value)
    }

    @Test fun concConfinedUnsubscribe() = confinedUnsubscribe(true)
    @Test fun unsConfinedUnsubscribe() = confinedUnsubscribe(false)

    private fun confinedUnsubscribe(concurrent: Boolean) {
        val called = concurrentPropertyOf(false)
        val f = ForkJoinPool.commonPool().submit {
            val prop = propertyOf("", concurrent)
            val deb = prop.debounced(10, TimeUnit.MILLISECONDS)
            val listener = { _: String, _: String ->
                called.value = true
            }
            deb.addChangeListener(listener)

            repeat(ForkJoinPool.getCommonPoolParallelism() + 1) {
                ForkJoinPool.commonPool().execute { Thread.sleep(25) }
            } // pool is full, our update task will be delayed

            prop.value = "new"

            Thread.sleep(15)

            deb.removeChangeListener(listener)
        }
        Thread.sleep(10) // let it start running
        f.get()
        Thread.sleep(10)
        assertFalse(called.value)
    }

}
