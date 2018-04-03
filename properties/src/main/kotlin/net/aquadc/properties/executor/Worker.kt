package net.aquadc.properties.executor

import java.util.concurrent.Executor

/**
 * A thing which executes tasks.
 */
interface Worker {

    fun <T, U> map(t: T, map: (T) -> U, callback: (U) -> Unit)
    fun <T, U, V> map2(t: T, u: U, map: (T, U) -> V, callback: (V) -> Unit)

}

/**
 * [Worker] which executes code in-place.
 */
object InPlaceWorker : Worker {

    override fun <T, U> map(t: T, map: (T) -> U, callback: (U) -> Unit) {
        callback(map(t))
    }

    override fun <T, U, V> map2(t: T, u: U, map: (T, U) -> V, callback: (V) -> Unit) {
        callback(map(t, u))
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

    override fun <T, U, V> map2(t: T, u: U, map: (T, U) -> V, callback: (V) -> Unit) {
        executor.execute {
            callback(map(t, u))
        }
    }

}
