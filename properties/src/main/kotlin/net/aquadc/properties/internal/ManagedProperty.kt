package net.aquadc.properties.internal

import android.support.annotation.RestrictTo
import net.aquadc.properties.TransactionalProperty

/**
 * A property whose value can be changed inside a transaction.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ManagedProperty<TRANSACTION, T, TOKN>(
        private val manager: Manager<TRANSACTION, TOKN, T>,
        private val token: TOKN,
        private val id: Long
) : `Notifier-1AtomicRef`<T, T>(true, unset()), TransactionalProperty<TRANSACTION, T> {

    override val value: T
        get() {
            // check for uncommitted changes
            val dirty = manager.getDirty(token, id)
            if (dirty !== Unset) return dirty

            // check cached
            val cached = ref
            if (cached !== Unset) return cached

            val clean = manager.getClean(token, id)
            refUpdater().lazySet(this, clean)
            return clean
        }

    override fun setValue(transaction: TRANSACTION, value: T) {
        val clean = if (ref === Unset) manager.getClean(token, id) else Unset
        // after mutating dirty state we won't be able to see the clean one, so preserve it

        manager.set(transaction, token, id, value)
        // this changes 'dirty' state (and value returned by 'get'),
        // but we don't want to deliver it until it becomes clean

        if (clean !== Unset) ref = clean as T // mutated successfully, preserve clean
    }

    fun commit(newValue: T) {
        if (newValue !== Unset) {
            // the transaction was committed

            val oldValue = ref
            ref = newValue

            valueChanged(
                    // when just created, oldValue === Unset, so just say we've changed from 'new' to 'new'
                    if (oldValue === Unset) newValue else oldValue,
                    newValue,
                    null
            )
        }
    }

}

/**
 * A manager of a property, e. g. a database session.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface Manager<TRANSACTION, TOKN, T> {

    /**
     * Returns dirty transaction value for current thread, or [Unset], if none.
     */
    fun getDirty(token: TOKN, id: Long): T

    /**
     * Returns clean value.
     */
    fun getClean(token: TOKN, id: Long): T

    /**
     * Sets 'dirty' value during [transaction].
     */
    fun set(transaction: TRANSACTION, token: TOKN, id: Long, update: T)

}
