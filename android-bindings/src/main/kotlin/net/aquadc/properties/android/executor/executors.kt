package net.aquadc.properties.android.executor

import android.os.Handler
import android.os.Looper
import android.os.MessageQueue
import java.util.concurrent.Executor


/**
 * A factory which creates an [Executor] for current [Looper].
 */
@Suppress("unused") // :properties is linked against :fake-bindings which contains a class with the same name
object AndroidCurrentLooperExecutorFactory : () -> Executor? {

    /**
     * Returns new [Handlecutor], if called on a thread with a [Looper].
     */
    override fun invoke(): Executor? =
            Looper.myLooper()?.let(::Handlecutor)

    override fun toString(): String =
            "AndroidCurrentLooperExecutorFactory(current looper: ${Looper.myLooper()})"

}

/**
 * [Executor] implementation merged with Android's [Handler].
 * Note: `fake-android` module has a class which mimics this class interface, keep in sync.
 */
class Handlecutor(looper: Looper) : Handler(looper), Executor {

    override fun execute(command: Runnable): Unit =
            check(post(command))

}

/**
 * [Executor] implementation based on Android's [Handler].
 * If you need new [Handler]/[Executor], prefer [Handlecutor] instead.
 */
class HandlerExecutor(
        private val handler: Handler
) : Executor {

    /**
     * Posts the given [command] into the [handler].
     */
    override fun execute(command: Runnable): Unit =
            check(handler.post(command))

}

/**
 * [Executor] implementation which executes tasks while the queue is idled.
 */
class IdleExecutor(
        private val queue: MessageQueue
) : Executor {

    /**
     * Posts given [command] into the [queue] to be processed in idle state.
     */
    override fun execute(command: Runnable) {
        queue.addIdleHandler {
            command.run()
            false
        }
    }

}
