package net.aquadc.properties.executor

import android.os.Handler
import android.os.Looper
import javafx.application.Platform
import java.util.concurrent.Executor

/**
 * [Executor] implementation merged with Android's [Handler].
 * Will cause [NoClassDefFoundError] if called out of Android.
 */
class Handlecutor(looper: Looper) : Handler(looper), Executor {

    override fun execute(command: Runnable) =
            check(post(command))

}

/**
 * Wraps [Platform] to run JavaFX Application Thread.
 * Will cause [NoClassDefFoundError] if called on platform without JavaFX.
 */
object FxApplicationThreadExecutor : Executor {

    override fun execute(command: Runnable) =
            Platform.runLater(command)

}
