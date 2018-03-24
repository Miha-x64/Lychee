package net.aquadc.properties.executor

import javafx.application.Platform
import java.util.concurrent.Executor

/**
 * Wraps [Platform] to run JavaFX Application Thread.
 */
object FxApplicationThreadExecutor : Executor {

    override fun execute(command: Runnable) =
            Platform.runLater(command)

}
