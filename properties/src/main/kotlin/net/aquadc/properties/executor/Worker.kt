package net.aquadc.properties.executor

import java.util.concurrent.Executor

/**
 * A thing which executes tasks.
 */
interface Worker {

    fun <T, U> map(t: T, map: (T) -> U, callback: (U) -> Unit)

}

/**
 * [Worker] which executes code in-place.
 */
object InPlaceWorker : Worker {

    override fun <T, U> map(t: T, map: (T) -> U, callback: (U) -> Unit) {
        callback(map(t))
    }

}

/**
 * [Worker] which posts code into the specified [Executor].
 */
class WorkerOnExecutor(private val executor: Executor) : Worker {

    override fun <T, U> map(t: T, map: (T) -> U, callback: (U) -> Unit) {
        executor.execute {
            callback(map(t))
        }
    }

}
