package net.aquadc.properties.android.executor

import java.util.concurrent.Executor


object AndroidCurrentLooperExecutorFactory : () -> Executor? {

    init {
        error("This class is only for compile-time.")
    }

    override fun invoke(): Executor? = error("")

}
