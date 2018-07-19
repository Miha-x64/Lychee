package net.aquadc.properties.fx

import javafx.application.Platform
import java.util.concurrent.Executor

/**
 * Wraps [Platform] to run tasks on JavaFX Application Thread.
 */
object JavaFxApplicationThreadExecutorFactory : () -> Executor?, Executor {

    init {
        Platform.isFxApplicationThread() // throw NoClassDefFoundError if not available
    }

    override fun invoke(): Executor? =
            if (Platform.isFxApplicationThread()) this else null

    override fun execute(command: Runnable) {
        Platform.runLater(command)
    }

    override fun toString(): String =
            "JavaFxApplicationThreadExecutorFactory(on application thread: ${Platform.isFxApplicationThread()})"

}
