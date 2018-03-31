package net.aquadc.properties.internal

@Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
internal class ConcListeners(
        @JvmField val notifying: Boolean,
        @JvmField val listeners: Array<Any?>,
        @JvmField val pendingValues: Array<Any?>
) {

    fun withListener(newListener: Any): ConcListeners =
            ConcListeners(notifying, listeners.with(newListener), pendingValues)

    fun withoutListener(victim: Any): ConcListeners {
        val idx = listeners.indexOf(victim)
        if (idx < 0) {
            return this
        }

        val newListeners = when {
            notifying -> listeners.clone().also { it[idx] = null }
            // we can't just remove this element while array is being iterated

            listeners.size == 1 -> EmptyArray
            // our victim was the only listener — let's return a shared const

            else -> listeners.copyOfWithout(idx, EmptyArray)
            // we're not the only listener, not notifying, remove at the specified position
        }

        return if (!notifying && newListeners.isEmpty() && pendingValues.isEmpty())
            NoListeners
        else
            ConcListeners(notifying, newListeners, pendingValues)
    }

    fun withNextValue(newValue: Any?): ConcListeners =
            if (notifying) {
                ConcListeners(notifying, listeners, pendingValues.with(newValue))
            } else {
                ConcListeners(true, listeners, pendingValues)
            }

    fun next(): ConcListeners {
        check(notifying)
        val notifyMore: Boolean
        val listeners: Array<Any?>
        val pendingValues: Array<Any?>

        if (this.pendingValues.isEmpty()) {
            notifyMore = false
            listeners = this.listeners.withoutNulls(EmptyArray)
            pendingValues = this.pendingValues /* empty array */
        } else {
            notifyMore = true
            listeners = this.listeners
            pendingValues = this.pendingValues.copyOfWithout(0, EmptyArray)
        }

        return ConcListeners(notifyMore, listeners, pendingValues)
    }

    companion object {
        @JvmField val EmptyArray = emptyArray<Any?>()
        @JvmField val NoListeners = ConcListeners(false, EmptyArray, EmptyArray)
    }

}
