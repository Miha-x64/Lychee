package net.aquadc.properties.internal

import net.aquadc.properties.ChangeListener

@Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
internal class ConcListeners(
        @JvmField val notifying: Boolean,
        @JvmField val listeners: Array<ChangeListener<Any?>?>,
        @JvmField val pendingValues: Array<Any?>
) {

    fun withListener(newListener: ChangeListener<*>): ConcListeners =
            ConcListeners(notifying, listeners.with(newListener as ChangeListener<Any?>), pendingValues)

    fun withoutListener(victim: ChangeListener<*>): ConcListeners {
        val idx = listeners.indexOf(victim as ChangeListener<Any?>)
        if (idx < 0) {
            return this
        }

        val newListeners = when {
            notifying -> listeners.clone().also { it[idx] = null }
            // we can't just remove this element while array is being iterated

            listeners.size == 1 -> EmptyListenersArray
            // our victim was the only listener — let's return a shared const

            else -> listeners.copyOfWithout(idx, EmptyListenersArray)
            // we're not the only listener, not notifying, remove at the specified position
        }

        return if (!notifying && newListeners.isEmpty() && pendingValues.isEmpty())
            NoListeners
        else
            ConcListeners(notifying, newListeners, pendingValues)
    }

    fun withNextValue(newValue: Any?) =
            if (notifying) {
                ConcListeners(notifying, listeners, pendingValues.with(newValue))
            } else {
                ConcListeners(true, listeners, pendingValues)
            }

    fun next(): ConcListeners {
        check(notifying)
        val notifyMore: Boolean
        val listeners: Array<ChangeListener<Any?>?>
        val pendingValues: Array<Any?>

        if (this.pendingValues.isEmpty()) {
            notifyMore = false
            listeners = this.listeners.withoutNulls(EmptyListenersArray)
            pendingValues = this.pendingValues /* empty array */
        } else {
            notifyMore = true
            listeners = this.listeners
            pendingValues = this.pendingValues.copyOfWithout(0, EmptyPendingValuesArray)
        }

        return ConcListeners(notifyMore, listeners, pendingValues)
    }

    companion object {
        @JvmField val EmptyListenersArray = emptyArray<ChangeListener<Any?>?>()
        @JvmField val EmptyPendingValuesArray = emptyArray<Any?>()
        @JvmField val NoListeners = ConcListeners(false, EmptyListenersArray, EmptyPendingValuesArray)
    }

}
