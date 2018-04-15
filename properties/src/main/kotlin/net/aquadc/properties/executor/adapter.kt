package net.aquadc.properties.executor

import net.aquadc.properties.ChangeListener
import java.util.concurrent.Executor

internal class MapWhenChanged<in T, U>(
        private val mapOn: Worker,
        private val map: (T) -> U,
        private val consumer: (U) -> Unit
) : ChangeListener<T> {

    override fun invoke(old: T, new: T) {
        mapOn.map(new, map, consumer)
    }

}

internal class ConsumeOn<in T>(
        private val consumeOn: Executor,
        private val consumer: (T) -> Unit
) : (T) -> Unit {
    override fun invoke(value: T) {
        consumeOn.execute {
            consumer(value)
        }
    }
}
