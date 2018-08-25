package net.aquadc.properties.internal

import android.support.annotation.RestrictTo
import net.aquadc.properties.TransactionalProperty
import java.lang.IllegalStateException

/**
 * A property whose value can be changed inside a transaction.
 *
 * Note: [manager] is not volatile and requires external synchronization if used concurrently
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ManagedProperty<TRANSACTION, T>(
        private var manager: Manager<TRANSACTION>?,
        private val column: Manager.Column<T>,
        private val id: Long
) : `Notifier-1AtomicRef`<T, T>(true, unset()), TransactionalProperty<TRANSACTION, T> {

    override val value: T
        get() {
            val manager = requireManaged()

            // check for uncommitted changes
            val dirty = manager.getDirty(column, id)
            if (dirty !== Unset) return dirty

            // check cached
            val cached = ref
            if (cached !== Unset) return cached

            val clean = manager.getClean(column, id)
            refUpdater().lazySet(this, clean)
            return clean
        }

    override fun setValue(transaction: TRANSACTION, value: T) {
        val manager = requireManaged()

        val clean = if (ref === Unset) manager.getClean(column, id) else Unset
        // after mutating dirty state we won't be able to see the clean one,
        // so we'll preserve it later in this method

        manager.set(transaction, column, id, value)
        // this changes 'dirty' state (and value returned by 'get'),
        // but we don't want to deliver it until it becomes clean

        if (clean !== Unset) ref = clean as T // mutated successfully, preserve clean
    }

    fun commit(newValue: T) {
        requireManaged()

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

    fun dropManagement() {
        manager = null
    }

    val isManaged: Boolean
        get() = manager != null

    private fun requireManaged(): Manager<TRANSACTION> =
            manager ?: throw IllegalStateException(
                    "${toString()} is not managed anymore, e. g. was removed from underlying storage" +
                            if (ref !== Unset) ". Last remembered value: '$ref'" else "")

    override fun toString(): String =
            "ManagedProperty(at $column#$id)"

}

/**
 * A manager of a property, e. g. a database session.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface Manager<TRANSACTION> {

    /**
     * Returns dirty transaction value for current thread, or [Unset], if none.
     */
    fun <T> getDirty(column: Column<T>, id: Long): T

    /**
     * Returns clean value.
     */
    fun <T> getClean(column: Column<T>, id: Long): T

    /**
     * Sets 'dirty' value during [transaction].
     */
    fun <T> set(transaction: TRANSACTION, column: Column<T>, id: Long, update: T)

    /**
     * Manager may manage a whole table and work with different types.
     * Column says which column/property/etc and which type.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    interface Column<T> // fixme: this API is not cool

}
