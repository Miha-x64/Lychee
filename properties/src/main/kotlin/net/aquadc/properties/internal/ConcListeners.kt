package net.aquadc.properties.internal

/**
 * This is internal API, despite the class is public.
 * @property listeners to avoid breaking iteration loop,
 *                     removed listeners becoming nulls while iterating
 * @property pendingValues a list of updates to deliver. If not empty,
 *                         then notification is happening right now
 */
@Suppress("UNCHECKED_CAST")
class ConcListeners<out L : Any, out T>(
        @JvmField val listeners: Array<out L?>,
        @JvmField val pendingValues: Array<out T>
) {

    fun withListener(newListener: @UnsafeVariance L): ConcListeners<L, T> =
            ConcListeners(listeners.with(newListener) as Array<L?>, pendingValues)

    fun withoutListener(victim: @UnsafeVariance L): ConcListeners<L, T> {
        val idx = listeners.indexOf(victim)
        if (idx < 0) {
            return this
        }

        val newListeners = when {
            pendingValues.isNotEmpty() ->
                (listeners as Array<L?>).clone().also { it[idx] = null }
            // we can't just remove this element while array is being iterated, nulling it out instead

            listeners.size == 1 ->
                EmptyArray as Array<L?>
            // our victim was the only listener, not notifying — returning a shared const

            else ->
                listeners.copyOfWithout(idx, EmptyArray) as Array<L?>
            // we're not the only listener, not notifying, remove at the specified position
        }

        return if (pendingValues.isEmpty() && newListeners.isEmpty() && pendingValues.isEmpty())
            NoListeners
        else
            ConcListeners(newListeners, pendingValues)
    }

    fun withNextValue(newValue: @UnsafeVariance T): ConcListeners<L, T> =
            ConcListeners(listeners, pendingValues.with(newValue) as Array<out T>)

    fun next(): ConcListeners<L, T> {
        val listeners = if (this.pendingValues.size == 1) {
            // 1 means we're stopping notification, will remove nulls then
            (this.listeners as Array<L?>).withoutNulls(EmptyArray as Array<L>)
        } else {
            this.listeners
        }

        // remove value at 0, that listeners were just notified about
        return ConcListeners(listeners, pendingValues.copyOfWithout(0, EmptyArray) as Array<out T>)
    }


    internal companion object {
        @JvmField val EmptyArray =
                emptyArray<Any?>()

        @JvmField val NoListeners =
                ConcListeners(EmptyArray, EmptyArray) as ConcListeners<Nothing, Nothing>
    }

}
