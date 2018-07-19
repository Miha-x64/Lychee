package net.aquadc.properties.executor

import net.aquadc.properties.android.executor.AndroidCurrentLooperExecutorFactory
import net.aquadc.properties.fx.JavaFxApplicationThreadExecutorFactory
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.ForkJoinTask
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadFactory
import kotlin.concurrent.getOrSet


internal object ScheduledDaemonHolder : ThreadFactory {

    @JvmField
    internal val scheduledDaemon =
            ScheduledThreadPoolExecutor(1, this)

    override fun newThread(r: Runnable): Thread =
            Thread(r).also { it.isDaemon = true }

}

internal object PlatformExecutors {
    @JvmField internal val executors = ThreadLocal<Executor>()
    private val executorFactories: Array<() -> Executor?>

    init {
        val facs = ArrayList<() -> Executor?>(2)

        try {
            facs.add(AndroidCurrentLooperExecutorFactory)
        } catch (ignored: NoClassDefFoundError) {
            // only Android has handlers, JDK doesn't
        }

        try {
            facs.add(JavaFxApplicationThreadExecutorFactory)
        } catch (ignored: NoClassDefFoundError) {
            // Android and some JDK builds do not contain JavaFX
        }

        try {
            ForkJoinTask.getPool() // ensure class available
            facs.add(object : () -> Executor? {
                override fun invoke(): Executor? {
                    val pool = ForkJoinTask.getPool() ?: return null
                    return if (pool.parallelism == 1) pool else null
                }

                override fun toString(): String {
                    val pool = ForkJoinTask.getPool() ?: return "ForkJoinPoolExecutorFactory(currently not on FJ pool)"
                    val parallelism = pool.parallelism
                    return "ForkJoinPoolExecutorFactory(current pool: $pool, parallelism=$parallelism" +
                            if (parallelism != 1) " (unsupported))" else ")"
                }
            })
        } catch (ignored: NoClassDefFoundError) {
            // only JDK 1.7+ and Android 21+ contain FJ
        }

        executorFactories = facs.toArray(arrayOfNulls(facs.size))
    }

    internal fun executorForCurrentThread(): Executor =
            executors.getOrSet(::createForCurrentThread)

    private fun createForCurrentThread(): Executor {
        executorFactories.forEach { it()?.let { return it } }
        throw UnsupportedOperationException(
                "Can't execute task on ${Thread.currentThread()}. " +
                        "Executor factories available: ${Arrays.toString(executorFactories)}")
    }

}
