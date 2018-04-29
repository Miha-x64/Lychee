package net.aquadc.properties.diff

import net.aquadc.properties.concurrentMutablePropertyOf
import net.aquadc.properties.executor.InPlaceWorker
import net.aquadc.properties.executor.WorkerOnExecutor
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class DiffProp {

    @Test fun calcilateDiffInPlace() {
        val prop = concurrentMutablePropertyOf(10)
        val diffProp = prop.calculateDiffOn(InPlaceWorker) { old, new -> new - old }
        var diff = 0
        val listener: DiffChangeListener<Int, Int> = { _, _, d -> diff = d }
        diffProp.addChangeListener(listener)

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
        val prop = concurrentMutablePropertyOf(10)
        val diffProp = prop.calculateDiffOn(worker) { old, new -> new - old }
        val diff = AtomicInteger()

        val listener: DiffChangeListener<Int, Int> = { _, _, d -> diff.set(d) }
        diffProp.addChangeListener(listener)

        prop.value = 100

        executor.submit {  }.get()
        executor.shutdown()

        assertEquals(90, diff.get())
        assertEquals(100, diffProp.value)

        diffProp.removeChangeListener(listener)
        prop.value = 100500
        assertEquals(90, diff.get())
    }

}
