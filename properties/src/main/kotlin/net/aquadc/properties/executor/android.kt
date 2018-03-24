package net.aquadc.properties.executor

import android.os.Handler
import java.util.concurrent.Executor

/**
 * [Executor] implementation based on Android's [Handler].
 * Will cause [NoClassDefFoundError] if called out of Android.
 */
class HandlerAsExecutor(
        private val handler: Handler
) : Executor {

    override fun execute(command: Runnable) =
            check(handler.post(command))

}
