package net.aquadc.properties.internal

import javafx.application.Platform
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater
import kotlin.collections.ArrayList


internal inline fun <T, V> AtomicReferenceFieldUpdater<T, V>.update(zis: T, update: (V) -> V) {
    var prev: V
    var next: V
    do {
        prev = get(zis)
        next = update(prev)
    } while (!compareAndSet(zis, prev, next))
}

object PlatformExecutors {
    private val executors = ConcurrentHashMap<Thread, Executor>()
    private val executorFactories: Array<(Thread) -> Executor?>

    init {
        val facs = ArrayList<(Thread) -> Executor?>(2)

        try {
            facs.add(Class.forName("net.aquadc.properties.android.LooperExecutorFactory").newInstance() as (Thread) -> Executor?)
        } catch (ignored: ClassNotFoundException) {}

        try {
            Platform.isFxApplicationThread() // ensure class available
            facs.add { if (Platform.isFxApplicationThread()) Executor(Platform::runLater) else null }
        } catch (ignored: NoClassDefFoundError) {}

        executorFactories = facs.toTypedArray()
    }

    internal fun executorForThread(thread: Thread): Executor {
        val ex = executors[thread]
        if (ex == null) {
            val newEx = thread.createExecutor()
            executors.putIfAbsent(thread, newEx)
        }
        return executors[thread]!!
    }

    private fun Thread.createExecutor(): Executor {
        executorFactories.forEach { it(this)?.let { return it } }
        throw UnsupportedOperationException("Can't schedule task on $this")
    }

}