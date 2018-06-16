package net.aquadc.properties.diff

import org.junit.Assert.assertEquals
import org.junit.AssumptionViolatedException
import org.junit.Test
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger


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

        repeat(100_000) {
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

}
