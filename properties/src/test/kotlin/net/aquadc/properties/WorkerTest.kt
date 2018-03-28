package net.aquadc.properties

import net.aquadc.properties.executor.InPlaceWorker
import net.aquadc.properties.executor.Worker
import net.aquadc.properties.executor.WorkerOnExecutor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executor
import java.util.concurrent.Executors


class WorkerTest {

    private val bgWrk: Worker
    private val exec: Executor
    private val bgThread: Thread

    init {
        var thread: Thread? = null
        exec = Executors.newSingleThreadExecutor { runnable -> Thread(runnable).also {
            it.isDaemon = true
            thread = it
        } }

        exec.execute { /* just trigger factory to set [thread] variable value */ }

        this.bgWrk = WorkerOnExecutor(exec)
        this.bgThread = thread!!
    }

    @Test fun mappedPropertyInPlace() = mappedProperty(InPlaceWorker, Thread.currentThread())
    @Test fun mappedPropertyOnBg() = mappedProperty(bgWrk, bgThread)

    private fun mappedProperty(worker: Worker, expectedThread: Thread) {
        val mappedOn = CopyOnWriteArrayList<Thread>()
        val mappedVals = CopyOnWriteArrayList<String>()

        val prop = concurrentMutablePropertyOf("none")
        val mapped = prop.mapOn(worker) {
            mappedOn.add(Thread.currentThread())
            mappedVals.add(it.toUpperCase())

            it.toUpperCase()
        }

        prop.value = "some"

        while (mapped.value == "NONE")
            Thread.yield()

        assertEquals("NONE", mappedVals[0])
        assertEquals("SOME", mappedVals[1])
        assertEquals("SOME", mapped.value)

        assertSame(Thread.currentThread(), mappedOn[0])
        assertSame(expectedThread, mappedOn[1])
    }

}
