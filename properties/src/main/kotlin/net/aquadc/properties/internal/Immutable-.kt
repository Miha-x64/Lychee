package net.aquadc.properties.internal

import net.aquadc.properties.ChangeListener
import net.aquadc.properties.Property
import java.util.concurrent.Executor

@PublishedApi
internal class `Immutable-`<out T>(
        override val value: T
) : Property<T> {

    override val mayChange: Boolean get() = false
    override val isConcurrent: Boolean get() = true

    override fun addChangeListener(onChange: (old: T, new: T) -> Unit) = Unit

    override fun addChangeListenerOn(executor: Executor, onChange: ChangeListener<T>) = Unit

    override fun removeChangeListener(onChange: (old: T, new: T) -> Unit) = Unit

    @PublishedApi
    internal companion object {
        @JvmField val TRUE = `Immutable-`(true)
        @JvmField val FALSE = `Immutable-`(false)
    }

}
