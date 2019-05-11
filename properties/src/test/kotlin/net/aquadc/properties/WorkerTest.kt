package net.aquadc.properties

import net.aquadc.properties.executor.InPlaceWorker
import net.aquadc.properties.executor.Worker
import net.aquadc.properties.executor.WorkerOnExecutor
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ForkJoinWorkerThread
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference


class WorkerTest {

    private val caller: ExecutorService
    private val callerThread: Thread

    private val worker: Worker<*>
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

    private fun mappedProperty(prop: MutableProperty<String>, caller: ExecutorService, callerThread: Thread, worker: Worker<*>, workerThread: Thread, notifyThread: Thread) {
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

    @Test(expected = IllegalStateException::class) fun `fail on background`() {
        val ex = AtomicReference<Throwable>()
        val exec = Executors.newSingleThreadExecutor {
            Thread().also {
                it.uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { _, e -> ex.set(e) }
            }
        }
        val prop = concurrentPropertyOf(10)
        val mapProp = prop.mapOn(WorkerOnExecutor(exec)) { _ -> throw IllegalStateException() }
        mapProp.addUnconfinedChangeListener { _, _ ->  }
        prop.value = 100500

        exec.shutdown()
        check(exec.awaitTermination(1, TimeUnit.SECONDS))
        ex.get()?.let { throw it }
    }

    @Test fun `cancel calculation`() {
        val called = AtomicBoolean()
        val exec = Executors.newSingleThreadExecutor()
        val prop = concurrentPropertyOf(10)
        val mapProp = prop.mapOn(WorkerOnExecutor(exec)) { _ -> called.set(true) }

        exec.execute { Thread.sleep(100) }
        val changeListener = { _: Unit, _: Unit -> }
        mapProp.addUnconfinedChangeListener(changeListener)

        called.set(false) // reset, called when added listener
        prop.value = 100500
        mapProp.removeChangeListener(changeListener)

        exec.shutdown()
        check(exec.awaitTermination(1, TimeUnit.SECONDS))
        assertEquals(false, called.get())
    }

}
