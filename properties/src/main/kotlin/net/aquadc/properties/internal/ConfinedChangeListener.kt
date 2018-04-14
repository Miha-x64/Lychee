package net.aquadc.properties.internal

import net.aquadc.properties.ChangeListener
import java.util.concurrent.Executor

class ConfinedChangeListener<in T>(
        private val executor: Executor,
        @JvmField internal val actual: ChangeListener<T>
) : ChangeListener<T> {

    override fun invoke(old: T, new: T) {
        executor.execute { actual(old, new) }
    }

}
