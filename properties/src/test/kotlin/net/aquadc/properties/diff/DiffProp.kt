package net.aquadc.properties.diff

import net.aquadc.properties.ChangeListener
import net.aquadc.properties.addUnconfinedChangeListener
import net.aquadc.properties.concurrentPropertyOf
import net.aquadc.properties.executor.InPlaceWorker
import net.aquadc.properties.executor.WorkerOnExecutor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class DiffProp {

    @Test fun calculateDiffInPlace() {
        val prop = concurrentPropertyOf(10)
        val diffProp = prop.calculateDiffOn(InPlaceWorker) { old, new -> new - old }
        var diff = 0
        val listener: DiffChangeListener<Int, Int> = { _, _, d -> diff = d }
        diffProp.addUnconfinedChangeListener(listener)

        prop.value = 100
        assertEquals(90, diff)
        assertEquals(100, diffProp.value)

        diffProp.removeChangeListener(listener)
        prop.value = 200
        assertEquals(90, diff) // nothing changed, we've unsubscribed
    }

    @Test fun calculateDiffInOnWorker() {
        val executor = Executors.newSingleThreadExecutor()
        val worker = WorkerOnExecutor(executor)
        val prop = concurrentPropertyOf(10)
        val diffProp = prop.calculateDiffOn(worker) { old, new -> new - old }
        val diff = AtomicInteger()

        val listener: DiffChangeListener<Int, Int> = { _, _, d -> diff.set(d) }
        diffProp.addUnconfinedChangeListener(listener)

        prop.value = 100

        executor.submit {  }.get()
        executor.shutdown()

        assertEquals(90, diff.get())
        assertEquals(100, diffProp.value)

        diffProp.removeChangeListener(listener)
        prop.value = 100500
        assertEquals(90, diff.get())
    }

    @Test fun `multi-arity`() {
        val prop = concurrentDiffPropertyOf<Int, Int>(0)

        var called2 = false
        var called3 = false

        val obj = object : ChangeListener<Int>, DiffChangeListener<Int, Int> {
            override fun invoke(old: Int, new: Int) {
                called2 = true
            }
            override fun invoke(old: Int, new: Int, diff: Int) {
                called3 = true
            }
        }

        val listener: ChangeListener<Int> = obj
        val diffListener: DiffChangeListener<Int, Int> = obj

        // should call only binary function
        prop.addUnconfinedChangeListener(listener)
        prop.casValue(0, 1, 1)
        assertTrue(called2)
        assertFalse(called3)

        called2 = false
        prop.removeChangeListener(listener)

        // should call only ternary one
        prop.addUnconfinedChangeListener(diffListener)
        prop.casValue(1, 2, 1)
        assertFalse(called2)
        assertTrue(called3)

        called3 = false
        prop.removeChangeListener(diffListener)

        prop.addUnconfinedChangeListener(listener)
        prop.addUnconfinedChangeListener(diffListener)
        prop.casValue(2, 3, 1)
        assertTrue(called2)
        assertTrue(called3)

        called2 = false
        called3 = false
        prop.removeChangeListener(listener)
        prop.removeChangeListener(diffListener)

        prop.addUnconfinedChangeListener(diffListener)
        prop.addUnconfinedChangeListener(listener)
        prop.casValue(3, 4, 1)
        assertTrue(called2)
        assertTrue(called3)
    }

}
