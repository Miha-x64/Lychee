package net.aquadc.properties.executor

import android.os.Handler
import android.os.Looper
import javafx.application.Platform
import java.util.concurrent.*


internal object ScheduledDaemonHolder {
    internal val scheduledDaemon =
            ScheduledThreadPoolExecutor(1, ThreadFactory { Thread(it).also { it.isDaemon = true } })
}

internal object PlatformExecutors {
    private val executors = ConcurrentHashMap<Thread, Executor>(4)
    private val executorFactories: Array<() -> Executor?>

    init {
        val facs = ArrayList<() -> Executor?>(2)

        try {
            Looper.myLooper() // ensure class available
            facs.add {
                Looper.myLooper()?.let { myLooper -> HandlerAsExecutor(Handler(myLooper)) }
            }
        } catch (ignored: NoClassDefFoundError) {}

        try {
            Platform.isFxApplicationThread() // ensure class available
            facs.add {
                if (Platform.isFxApplicationThread()) FxApplicationThreadExecutor else null
            }
        } catch (ignored: NoClassDefFoundError) {}

        executorFactories = facs.toTypedArray()
    }

    internal fun executorForCurrentThread(): Executor {
        val thread = Thread.currentThread()

        val ex = executors[thread]
        if (ex != null) return ex

        val newEx = createForCurrentThread()

        // if putIfAbsent returns non-null value, the executor was set concurrently,
        // use it and throw away our then
        return executors.putIfAbsent(thread, newEx) ?: newEx
    }

    private fun createForCurrentThread(): Executor {
        executorFactories.forEach { it()?.let { return it } }
        throw UnsupportedOperationException("Can't execute task on ${Thread.currentThread()}")
    }

}
