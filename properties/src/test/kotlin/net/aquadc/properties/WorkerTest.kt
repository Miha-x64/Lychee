package net.aquadc.properties

import net.aquadc.properties.executor.InPlaceWorker
import net.aquadc.properties.executor.Worker
import net.aquadc.properties.executor.WorkerOnExecutor
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.*


class WorkerTest {

    private val caller: ExecutorService
    private val callerThread: Thread

    private val worker: Worker
    private val workerThread: Thread

    init {
        var clThread: Thread? = null
        this.caller = ForkJoinPool(1, { pool -> object : ForkJoinWorkerThread(pool) {}.also {
            it.isDaemon = true
            clThread = it
        } }, null, false)
        caller.execute { /* just trigger factory to set [clThread] variable value */ }
        this.callerThread = clThread!!


        var wrkThread: Thread? = null
        val exec = Executors.newSingleThreadExecutor { runnable -> Thread(runnable).also {
            it.isDaemon = true
            wrkThread = it
        } }

        exec.execute { /* just trigger factory to set [wrkThread] variable value */ }

        this.worker = WorkerOnExecutor(exec)
        this.workerThread = wrkThread!!
    }

    @Test fun unsMappedPropertyInPlace() =
            mappedProperty(
                    caller.submit<MutableProperty<String>> { propertyOf("none") }.get(),
                    caller, callerThread, InPlaceWorker, callerThread, callerThread
            )

    @Test fun unsMappedPropertyOnBg() =
            mappedProperty(
                    caller.submit<MutableProperty<String>> { propertyOf("none") }.get(),
                    caller, callerThread, worker, workerThread, callerThread
            )

    @Test fun concMappedPropertyInPlace() =
            mappedProperty(
                    concurrentPropertyOf("none"),
                    caller, callerThread, InPlaceWorker, callerThread, callerThread
            )

    @Test fun concMappedPropertyOnBg() =
            mappedProperty(
                    concurrentPropertyOf("none"),
                    caller, callerThread, worker, workerThread, workerThread
            )

    private fun mappedProperty(prop: MutableProperty<String>, caller: ExecutorService, callerThread: Thread, worker: Worker, workerThread: Thread, notifyThread: Thread) {
        val mappedOn = CopyOnWriteArrayList<Thread>()
        val mappedVals = CopyOnWriteArrayList<String>()
        val notifications = CopyOnWriteArrayList<String>()
        val notifiedOn = CopyOnWriteArrayList<Thread>()

        val lock = Semaphore(0)
        lateinit var mapped: Property<String>

        caller.submit {
            mapped = prop.mapOn(worker) {
                mappedOn.add(Thread.currentThread())

                it.toUpperCase().also { mappedVals.add(it) }
            }

            mapped.addUnconfinedChangeListener { _, new ->
                notifiedOn.add(Thread.currentThread())
                notifications.add(new)
                lock.release()
            }

            prop.value = "some"
        }.get()

        lock.acquire()

        caller.submit<Unit> {
            assertEquals(mappedVals.toString(), "SOME", mapped.value)
        }.get()

        assertEquals(listOf("NONE", "SOME"), mappedVals)
        assertEquals(listOf(callerThread, workerThread), mappedOn)
        assertEquals(listOf(notifyThread), notifiedOn)
        assertEquals(listOf("SOME"), notifications)
    }

}
