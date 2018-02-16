package net.aquadc.properties.android

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executor

/**
 * Creates [Executor] for given Android [Looper] [Thread].
 * @see net.aquadc.properties.internal.executorForThread
 */
class LooperExecutorFactory : (Thread) -> Executor? {
    override fun invoke(p1: Thread): Executor? {
        val my = Looper.myLooper()
                ?: return null

        val handler = Handler(my)
        return Executor { handler.post(it) }
    }
}
