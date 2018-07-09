package net.aquadc.properties

import net.aquadc.properties.diff.calculateDiffOn
import net.aquadc.properties.executor.InPlaceWorker
import net.aquadc.properties.executor.UnconfinedExecutor
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.Executors


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

        prop.value = false
        assertFalse(l2Called)
    }

    @Test fun confinedSubscribe() {
        val prop = concurrentPropertyOf(false)

        var l2Called by concurrentPropertyOf(false)

        val l2 = { _: Boolean, _: Boolean ->
            l2Called = true
        }

        val pool = Executors.newSingleThreadExecutor()

        val l1 = { _: Boolean, _: Boolean ->
            prop.addChangeListenerOn(pool, l2)
            Unit
        }

        prop.addUnconfinedChangeListener(l1)

        prop.value = false
        Thread.sleep(10)
        assertTrue(l2Called)

        l2Called = false
        prop.removeChangeListener(l1)

        assertTrue(prop.casValue(false, true))
        assertFalse(prop.casValue(false, true))
        Thread.sleep(10)
        assertTrue(l2Called)

        l2Called = false
        prop.removeChangeListener(l2)

        prop.value = false
        Thread.sleep(10)
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

    @Test fun concConfinedUnsubscribe() = confinedUnsubscribe(true, true)
    @Test fun unsConfinedUnsubscribe() = confinedUnsubscribe(false, true)

    @Test fun concUnconfinedUnsubscribe() = confinedUnsubscribe(true, false)
    @Test fun unsUnconfinedUnsubscribe() = confinedUnsubscribe(false, false)

    private fun confinedUnsubscribe(concurrent: Boolean, confined: Boolean) {
        val called = concurrentPropertyOf(false)
        val pool = Executors.newSingleThreadExecutor()
        val prop = propertyOf("", concurrent)
        val mapped = prop.readOnlyView()
        val listener = { _: String, _: String ->
            called.value = true
        }

        mapped.addChangeListenerOn(if (confined) pool else UnconfinedExecutor, listener)

        pool.execute { Thread.sleep(10) }

        prop.value = "new"

        Thread.sleep(10)

        mapped.removeChangeListener(listener)

        Thread.sleep(10)
        val shouldBeCalled = !confined
        assertEquals(shouldBeCalled, called.value)
        pool.shutdown()
    }

    @Test fun confinedDiff() = diff(true)
    @Test fun unconfinedDiff() = diff(false)

    private fun diff(confined: Boolean) {
        val called = concurrentPropertyOf(false)
        val pool = Executors.newSingleThreadExecutor()
        val prop = propertyOf(0)
        val diff = prop.calculateDiffOn(InPlaceWorker) { old, new -> new - old }
        val listener = { _: Int, _: Int, _: Int ->
            called.value = true
        }

        diff.addChangeListenerOn(if (confined) pool else UnconfinedExecutor, listener)

        pool.execute {
            Thread.sleep(25)
        }

        prop.value = 1

        Thread.sleep(15)

        diff.removeChangeListener(listener)

        Thread.sleep(30)
        val shouldBeCalled = !confined
        assertEquals(shouldBeCalled, called.value)
        pool.shutdown()
    }

}
