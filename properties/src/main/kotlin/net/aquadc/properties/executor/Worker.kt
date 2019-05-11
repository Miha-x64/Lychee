package net.aquadc.properties.executor

import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future

/**
 * A thing which executes tasks.
 */
interface Worker<F : Any> {

    /**
     * Invokes [map] on [t] and passes returned value to the [callback].
     */
    fun <T, U> map(t: T, map: (T) -> U, callback: (U) -> Unit): F

    /**
     * Invokes [map] on [t] and [u], passes returned value to the [callback].
     */
    fun <T, U, V> map2(t: T, u: U, map: (T, U) -> V, callback: (T, U, V) -> Unit): F

    /**
     * Interrupts execution of a task associated with a [future].
     */
    fun cancel(future: F)

}

/**
 * [Worker] which executes code in-place.
 */
object InPlaceWorker : Worker<Unit> {

    override fun <T, U> map(t: T, map: (T) -> U, callback: (U) -> Unit) {
        callback(map(t))
    }

    override fun <T, U, V> map2(t: T, u: U, map: (T, U) -> V, callback: (T, U, V) -> Unit) {
        callback(t, u, map(t, u))
    }

    override fun cancel(future: Unit) {
        // useless
    }

}

/**
 * [Worker] which posts code into the specified [Executor].
 */
class WorkerOnExecutor(
        private val executor: ExecutorService
) : Worker<Future<*>> {

    override fun <T, U> map(t: T, map: (T) -> U, callback: (U) -> Unit): Future<*> =
            executor.submit(Mapper<T, U, Nothing>(t, null, map, null, callback, null))

    override fun <T, U, V> map2(t: T, u: U, map: (T, U) -> V, callback: (T, U, V) -> Unit): Future<*> =
            executor.submit(Mapper(t, u, null, map, null, callback))

    override fun cancel(future: Future<*>) {
        future.cancel(true)
    }

}

private class Mapper<T, U, V>(
        private val t: T,
        private val u: U?,
        private val map1: ((T) -> U)?,
        private val map2: ((T, U) -> V)?,
        private val callback1: ((U) -> Unit)?,
        private val callback2: ((T, U, V) -> Unit)?
) : Runnable {

    override fun run() {
        try {
            if (map1 !== null) {
                callback1!!(map1.invoke(t))
            } else {
                callback2!!(t, u as U, map2!!(t, u))
            }
        } catch (e: Throwable) {
            Thread.currentThread().uncaughtExceptionHandler.uncaughtException(Thread.currentThread(), e)
        }
    }

}
