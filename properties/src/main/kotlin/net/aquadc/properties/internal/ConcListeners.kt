package net.aquadc.properties.internal

/**
 * This is internal API, despite the class is public.
 */
@Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
class ConcListeners<out T>(
        @JvmField val notifying: Boolean,
        @JvmField val listeners: Array<Any?>,
        @JvmField val pendingValues: Array<out T>
) {

    fun withListener(newListener: Any): ConcListeners<T> =
            ConcListeners(notifying, listeners.with(newListener), pendingValues)

    fun withoutListener(victim: Any): ConcListeners<T> {
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

    fun withNextValue(newValue: Any?): ConcListeners<T> =
            if (notifying) {
                ConcListeners(notifying, listeners, pendingValues.with(newValue) as Array<out T>)
            } else {
                ConcListeners(true, listeners, pendingValues)
            }

    fun next(): ConcListeners<T> {
        check(notifying)
        val notifyMore: Boolean
        val listeners: Array<Any?>
        val pendingValues: Array<out T>

        if (this.pendingValues.isEmpty()) {
            notifyMore = false
            listeners = this.listeners.withoutNulls(EmptyArray)
            pendingValues = this.pendingValues /* empty array */
        } else {
            notifyMore = true
            listeners = this.listeners
            pendingValues = this.pendingValues.copyOfWithout(0, EmptyArray) as Array<out T>
        }

        return ConcListeners(notifyMore, listeners, pendingValues)
    }

    companion object {
        @JvmField val EmptyArray = emptyArray<Any?>()
        @JvmField val NoListeners = ConcListeners(false, EmptyArray, EmptyArray) as ConcListeners<Nothing>
    }

}
