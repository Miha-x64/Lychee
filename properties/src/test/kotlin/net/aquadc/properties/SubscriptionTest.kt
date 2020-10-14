package net.aquadc.properties

import net.aquadc.properties.diff.calculateDiffOn
import net.aquadc.properties.executor.InPlaceWorker
import net.aquadc.properties.executor.UnconfinedExecutor
import net.aquadc.properties.function.identity
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.Executors


class SubscriptionTest {

    @Test fun concSubscribe() = subscribe(true)
    @Test fun unsSubscribe() = subscribe(false)

    private fun subscribe(conc: Boolean) {
        val prop = propertyOf(false, conc)

        var l2Called = 0

        val l2 = { _: Boolean, _: Boolean ->
            l2Called++; Unit
        }

        val l1 = { _: Boolean, _: Boolean ->
            prop.addUnconfinedChangeListener(l2)
        }

        prop.addUnconfinedChangeListener(l1)

        prop.value = false

        // l2 was added during l1 notification.
        // It had a chance to see the last Property.value and thus doesn't need notification
        assertEquals(0, l2Called)

        prop.removeChangeListener(l1)

        assertTrue(prop.casValue(false, true))
        assertFalse(prop.casValue(false, true))
        assertEquals(1, l2Called) // 1 CAS succeeded

        prop.removeChangeListener(l2)

        prop.value = false
        assertEquals(1, l2Called) // we've unsubscribed, must not increment
    }

    @Test fun confinedSubscribe() {
        val prop = concurrentPropertyOf(false)

        val l2Called = concurrentPropertyOf(0)

        val l2 = { _: Boolean, _: Boolean ->
            l2Called.update { it+1 }; Unit
        }

        val pool = Executors.newSingleThreadExecutor()

        val l1 = { _: Boolean, _: Boolean ->
            prop.addChangeListenerOn(pool, l2)
        }

        prop.addUnconfinedChangeListener(l1)

        prop.value = false
        Thread.sleep(10)
        assertEquals(0, l2Called.value)

        prop.removeChangeListener(l1)

        assertTrue(prop.casValue(false, true))
        assertFalse(prop.casValue(false, true))
        Thread.sleep(10)
        assertEquals(1, l2Called.value)

        prop.removeChangeListener(l2)

        prop.value = false
        Thread.sleep(10)
        assertEquals(1, l2Called.value)
    }

    @Test fun concUnsubscribe() = unsubscribe(true)
    @Test fun unsUnsubscribe() = unsubscribe(false)

    private fun unsubscribe(conc: Boolean) {
        val prop = propertyOf(false, conc)

        var l1Called = 0
        var l2Called = 0
        var l3Called = 0

        val l1 = { _: Boolean, _: Boolean ->
            l1Called++; Unit
        }

        val l3 = { _: Boolean, _: Boolean ->
            l3Called++; Unit
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
        var l1Called = 0
        var v2 = 0
        var l2Called = 0

        val l2 = { _: Int, new: Int ->
            l2Called++
            v2 = new
            if (new < 20) prop.value = 20
        }

        val l1 = { _: Int, new: Int ->
            l1Called++
            v1 = new
            if (new < 10) prop.value = 10
            else if (new < 20) prop.value = 20
        }

        prop.addUnconfinedChangeListener(l1)
        prop.addUnconfinedChangeListener(l2)

        prop.value = 1

        /*
        notify(1): queue=[10, 20]
        notify(10): queue=[20, 20, 20]
        notify(20): queue=[20, 20]
        notify(20): queue=[20]
        notify(20)
         */
        assertEquals(5, l1Called)
        assertEquals(5, l2Called)

        assertEquals(20, v1)
        assertEquals(20, v2)
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
        val mapped = prop.map(identity())
        val listener = { _: String, _: String ->
            called.value = true
        }

        mapped.addChangeListenerOn(if (confined) pool else UnconfinedExecutor, listener)

        pool.execute { Thread.sleep(20) }

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

        val state = concurrentPropertyOf(0)
        state as java.lang.Object

        pool.execute {
            // make the pool busy
            while (state.value == 0) synchronized(state, state::wait)
            check(state.casValue(1, 2))
            synchronized(state, state::notify)
        }

        prop.value = 1 // unconfined will notify in-place

        diff.removeChangeListener(listener) // confined won't notify

        check(state.casValue(0, 1)) // let pool go
        synchronized(state, state::notify)

        if (confined) { // and wait for it
            while (state.value == 1) synchronized(state, state::wait)
            Thread.sleep(10)
        }

        val shouldBeCalled = !confined
        assertEquals(shouldBeCalled, called.value)
        pool.shutdown()
    }

}
