package net.aquadc.properties.internal

/**
 * This is internal API, despite the class is public.
 */
@Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
class ConcListeners<out L : Any, out T>(
        @JvmField val notifying: Boolean,
        @JvmField val listeners: Array<out L?>,
        @JvmField val pendingValues: Array<out T>
) {

    fun withListener(newListener: @UnsafeVariance L): ConcListeners<L, T> =
            ConcListeners(notifying, listeners.with(newListener) as Array<L?>, pendingValues)

    fun withoutListener(victim: @UnsafeVariance L): ConcListeners<L, T> {
        val idx = listeners.indexOf(victim)
        if (idx < 0) {
            return this
        }

        val newListeners = when {
            notifying -> (listeners as Array<L?>).clone().also { it[idx] = null }
            // we can't just remove this element while array is being iterated

            listeners.size == 1 -> EmptyArray as Array<L?>
            // our victim was the only listener — let's return a shared const

            else -> listeners.copyOfWithout(idx, EmptyArray) as Array<L?>
            // we're not the only listener, not notifying, remove at the specified position
        }

        return if (!notifying && newListeners.isEmpty() && pendingValues.isEmpty())
            NoListeners
        else
            ConcListeners(notifying, newListeners, pendingValues)
    }

    fun withNextValue(newValue: @UnsafeVariance T): ConcListeners<L, T> =
            ConcListeners(true, listeners, pendingValues.with(newValue) as Array<out T>)

    fun next(): ConcListeners<L, T> {
        check(notifying)
        val notifyMore: Boolean
        val listeners: Array<out L?>
        val pendingValues: Array<out T>

        println(this.pendingValues.asList())
        if (this.pendingValues.size == 1) { // 1 means empty
            notifyMore = false
            listeners = (this.listeners as Array<L?>).withoutNulls(EmptyArray as Array<L>)
            pendingValues = EmptyArray as Array<out T>
        } else { // remove value that listeners were notified about
            notifyMore = true
            listeners = this.listeners
            pendingValues = this.pendingValues.copyOfWithout(0, EmptyArray) as Array<out T>
        }

        return ConcListeners(notifyMore, listeners, pendingValues)
    }

    companion object {
        @JvmField val EmptyArray =
                emptyArray<Any?>()

        @JvmField val NoListeners =
                ConcListeners(false, EmptyArray, EmptyArray) as ConcListeners<Nothing, Nothing>
    }

}
