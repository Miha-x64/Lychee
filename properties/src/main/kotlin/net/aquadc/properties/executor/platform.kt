package net.aquadc.properties.executor

import android.os.Handler
import javafx.application.Platform
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

/**
 * Wraps [Platform] to run JavaFX Application Thread.
 */
object FxApplicationThreadExecutor : Executor {

    override fun execute(command: Runnable) =
            Platform.runLater(command)

}
