package net.aquadc.properties.diff

import net.aquadc.properties.concurrentPropertyOf
import net.aquadc.properties.executor.InPlaceWorker
import net.aquadc.properties.executor.WorkerOnExecutor
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference


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

    @Test(expected = IllegalStateException::class) fun `fail on background`() {
        val ex = AtomicReference<Throwable>()
        val exec = Executors.newSingleThreadExecutor {
            Thread().also {
                it.uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { _, e -> ex.set(e) }
            }
        }
        val prop = concurrentPropertyOf(10)
        val diffProp = prop.calculateDiffOn(WorkerOnExecutor(exec)) { _, _ -> throw IllegalStateException() }
        diffProp.addUnconfinedChangeListener { _, _, _ ->  }
        prop.value = 100500

        exec.shutdown()
        check(exec.awaitTermination(1, TimeUnit.SECONDS))
        ex.get()?.let { throw it }
    }

    @Test fun `cancel calculation`() {
        val called = AtomicBoolean()
        val exec = Executors.newSingleThreadExecutor()
        val prop = concurrentPropertyOf(10)
        val diffProp = prop.calculateDiffOn(WorkerOnExecutor(exec)) { _, _ -> called.set(true) }

        exec.execute { Thread.sleep(100) }
        val changeListener = { _: Int, _: Int, _: Unit -> }
        diffProp.addUnconfinedChangeListener(changeListener)

        prop.value = 100500
        diffProp.removeChangeListener(changeListener)

        exec.shutdown()
        check(exec.awaitTermination(1, TimeUnit.SECONDS))
        assertEquals(false, called.get())
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
