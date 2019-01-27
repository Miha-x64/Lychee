package net.aquadc.properties.diff

import net.aquadc.properties.addUnconfinedChangeListener
import net.aquadc.properties.concurrentPropertyOf
import org.junit.Assert.assertEquals
import org.junit.AssumptionViolatedException
import org.junit.Test
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread


class Contended {

    @Test fun allDeliveredOnHalfCores() = halfCores(::allDelivered)
    @Test fun allDeliveredOnAllCores() = allCores(::allDelivered)
    @Test fun allDeliveredOnContentionForCores() = contentionForCores(::allDelivered)

    private fun halfCores(func: (Int) -> Unit) {
        val cores = Runtime.getRuntime().availableProcessors()
        if (cores < 2) throw AssumptionViolatedException("This test requires >1 hardware cores.")

        func(cores / 2)
    }
    private fun allCores(func: (Int) -> Unit) {
        val cores = Runtime.getRuntime().availableProcessors()
        if (cores < 2) throw AssumptionViolatedException("This test requires >1 hardware cores.")

        func(cores)
    }
    private fun contentionForCores(func: (Int) -> Unit) {
        func(4 * Runtime.getRuntime().availableProcessors())
    }

    private fun allDelivered(threads: Int) {
        val pool = Executors.newFixedThreadPool(threads)
        val prop = concurrentMutableDiffPropertyOf<Int?, Int?>(0)
        val sum = AtomicInteger()

        prop.addUnconfinedChangeListener { _, _, diff ->
            sum.addAndGet(diff!!)
        }

        repeat(10_000) {
            pool.execute {
                try {
                    do {
                        val value = prop.value
                        val new = value!! + 1
                    } while (!prop.casValue(value, new, 1))
                } catch (e: Exception) {
                    e.printStackTrace()
                    System.exit(1)
                }
            }
        }

        pool.shutdown()
        check(pool.awaitTermination(60, TimeUnit.SECONDS))
        assertEquals(100_000, sum.get())
    }

    @Test fun casDuringNotification() {
        val prop = concurrentPropertyOf(0)
        prop.addUnconfinedChangeListener { _, _ -> Thread.sleep(100) }
        val v0s = concurrentPropertyOf(false)
        thread {
            val v0 = prop.value
            v0s.value = prop.casValue(v0, v0 + 10)
        }
        val v1 = prop.value
        val v1s = prop.casValue(v1, v1 + 10)

        val v2 = prop.value
        val v2s = prop.casValue(v2, v2 + 10)

        val v3 = prop.value
        val v3s = prop.casValue(v3, v3 + 10)

        assertEquals(10 * (v0s.value.i + v1s.i + v2s.i + v3s.i), prop.value)
    }

    private val Boolean.i
        get() = if (this) 1 else 0

}
