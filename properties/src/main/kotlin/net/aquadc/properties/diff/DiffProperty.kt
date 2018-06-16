package net.aquadc.properties.diff

import net.aquadc.properties.Property
import java.util.concurrent.Executor

/**
 * Represents a property which provides a diff along with value changes.
 */
interface DiffProperty<out T, out D> : Property<T> {

    /**
     * Subscribe on value changes on current thread.
     * Acts like [MutableList.add]: [onChangeWithDiff] will be added even if it is already added.
     * @throws UnsupportedOperationException if this thread's executor cannot be identified
     */
    fun addChangeListener(onChangeWithDiff: DiffChangeListener<T, D>)

    /**
     * Subscribe on value changes on [executor].
     * Acts like [MutableList.add]: [onChangeWithDiff] will be added even if it is already added.
     */
    fun addChangeListenerOn(executor: Executor, onChangeWithDiff: DiffChangeListener<T, D>)

    /**
     * Unsubscribe from value changes.
     * Acts like [MutableList.remove]: removes only first occurrence of [onChangeWithDiff], if any.
     * But checks equality by identity, not with [equals].
     */
    fun removeChangeListener(onChangeWithDiff: DiffChangeListener<T, D>)

}
