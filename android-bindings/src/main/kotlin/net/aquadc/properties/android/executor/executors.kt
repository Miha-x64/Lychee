package net.aquadc.properties.android.executor

import android.os.Handler
import android.os.MessageQueue
import java.util.concurrent.Executor

/**
 * [Executor] implementation based on Android's [Handler].
 */
class HandlerExecutor(
        private val handler: Handler
) : Executor {

    /**
     * Posts the given [command] into the [handler].
     */
    override fun execute(command: Runnable) =
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
