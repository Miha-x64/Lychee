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
    private val executors = ThreadLocal<Executor>()
    private val executorFactories: Array<() -> Executor?>

    init {
        val facs = ArrayList<() -> Executor?>(2)

        findAndroidFactory(facs)
        findFxFactory(facs)
        findFjFactory(facs)
        executorFactories = facs.toArray(arrayOfNulls(facs.size))
    }

    private fun findAndroidFactory(facs: ArrayList<() -> Executor?>) {
        try {
            facs.add(AndroidCurrentLooperExecutorFactory)
        } catch (ignored: NoClassDefFoundError) {
            // only Android has handlers, JDK doesn't
        }
    }

    private fun findFxFactory(facs: ArrayList<() -> Executor?>) {
        try {
            facs.add(JavaFxApplicationThreadExecutorFactory)
        } catch (ignored: NoClassDefFoundError) {
            // Android and some JDK builds do not contain JavaFX
        }
    }

    private fun findFjFactory(facs: ArrayList<() -> Executor?>) {
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
    }

    internal fun requireCurrent(): Executor =
            getCurrent() ?: throw UnsupportedOperationException(
                    "Can't execute task on ${Thread.currentThread()}. " +
                            "Executor factories available: ${executorFactories.contentToString()}")

    internal fun getCurrent(): Executor? =
            executors.getOrSet {
                val facs = executorFactories
                for (i in facs.indices) {
                    facs[i]()?.let { return@getOrSet it }
                }
                return null
            }

}
