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
    
    @Test fun unsubscription() {
        val prop = concurrentDiffPropertyOf<Int, Int>(0)
        val listener: DiffChangeListener<Any?, Any?> = { _, _, _ ->
            fail()
        }
        prop.addUnconfinedChangeListener(listener)
        prop.removeChangeListener(listener)
        check(prop.casValue(0, 1, 1))
    }

}
