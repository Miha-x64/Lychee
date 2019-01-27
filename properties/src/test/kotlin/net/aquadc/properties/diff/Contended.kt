package net.aquadc.properties.diff

import net.aquadc.properties.addUnconfinedChangeListener
import net.aquadc.properties.concurrentPropertyOf
import net.aquadc.properties.getValue
import net.aquadc.properties.propertyOf
import net.aquadc.properties.setValue
import org.junit.Assert.assertEquals
import org.junit.AssumptionViolatedException
import org.junit.Test
import java.util.Vector
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.ArrayList
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
        assertEquals(10_000, sum.get())
    }

    @Test fun casDuringNotification() {
        val prop = concurrentPropertyOf(0)

        var state by concurrentPropertyOf(0)
        val monitor = java.lang.Object()
        prop.addUnconfinedChangeListener { _, _ ->
            state = 1
            synchronized(monitor, monitor::notify)
            while (state == 1) synchronized(monitor, monitor::wait)
        }

        val v0s = concurrentPropertyOf(false)
        thread {
            val v0 = prop.value
            v0s.value = prop.casValue(v0, v0 + 10)
            // listener triggered, hang...
        }

        while (state == 0) synchronized(monitor, monitor::wait) // await the beginning of a parallel thread

        val v1 = prop.value
        val v1s = prop.casValue(v1, v1 + 10)

        val v2 = prop.value
        val v2s = prop.casValue(v2, v2 + 10)

        val v3 = prop.value
        val v3s = prop.casValue(v3, v3 + 10)

        state = 2
        synchronized(monitor, monitor::notify) // don't sleep anymore

        assertEquals(10 * (v0s.value.i + v1s.i + v2s.i + v3s.i), prop.value)
    }

    private val Boolean.i
        get() = if (this) 1 else 0

    @Test fun subscriptionDuringParallelNotification() {
        val prop = concurrentPropertyOf(0)
        val monitor = java.lang.Object()
        var state by concurrentPropertyOf(0)
        val onChange: (Int, Int) -> Unit = { _, _ ->
            state = 1
            synchronized(monitor) {
                monitor.notify()
                while (state == 1) monitor.wait()
            }
        }
        prop.addUnconfinedChangeListener(onChange)
        val thr = thread { prop.value = 1 }

        while (state == 0) synchronized(monitor, monitor::wait) // wait until other thread suspends
        prop.removeChangeListener(onChange)

        // okay, other thread suspended inside notification
        prop.value = 2
        prop.value = 3
        prop.value = 4
        prop.value = 5

        val vals = Vector<Int>()
        prop.addUnconfinedChangeListener { _, new -> vals.add(new) }

        prop.value = 6
        prop.value = 7
        prop.value = 7
        prop.value = 6
        prop.value = 9
        prop.value = 10

        state = 2 // unlock thread, let it send all the notifications
        synchronized(monitor, monitor::notify)
        thr.join() // and await them

        assertEquals(listOf(6, 7, 7, 6, 9, 10), vals)
    }

    @Test fun unsSubscriptionDuringNotification() = subscriptionDuringNotification(false)
    @Test fun concSubscriptionDuringNotification() = subscriptionDuringNotification(true)
    private fun subscriptionDuringNotification(concurrent: Boolean) {
        val prop = propertyOf(0, concurrent)
        val vals = ArrayList<Int>()
        var onChange: (Int, Int) -> Unit = { _, _ -> }
        onChange = { _, _ ->
            prop.removeChangeListener(onChange)
            prop.value = 2
            prop.value = 3
            prop.value = 4
            prop.value = 5

            prop.addUnconfinedChangeListener { _, new -> vals.add(new) }

            prop.value = 6
            prop.value = 7
            prop.value = 7
            prop.value = 6
            prop.value = 9
            prop.value = 10
        }
        prop.addUnconfinedChangeListener(onChange)
        prop.value = 1

        assertEquals(listOf(6, 7, 7, 6, 9, 10), vals)
    }

}
