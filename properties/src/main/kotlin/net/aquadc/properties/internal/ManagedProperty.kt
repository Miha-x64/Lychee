package net.aquadc.properties.internal

import android.support.annotation.RestrictTo
import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.Schema
import net.aquadc.properties.TransactionalProperty

/**
 * A property whose value can be changed inside a transaction.
 *
 * Note: [manager] is not volatile and requires external synchronization if used (e. g. [dropManagement]) concurrently
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
open class ManagedProperty<SCH : Schema<SCH>, TRANSACTION, T, ID>(
        private var manager: Manager<SCH, TRANSACTION, ID>?,
        private val field: FieldDef<SCH, T>,
        private val id: ID,
        initialValue: T
) : `Notifier-1AtomicRef`<T, T>(true, initialValue), TransactionalProperty<TRANSACTION, T> {

    override val value: T
        get() {
            val manager = requireManaged()

            // check for uncommitted changes
            if (this.field is FieldDef.Mutable) {
                val dirty = manager.getDirty(this.field, id)
                if (dirty !== Unset) return dirty
            }

            // check cached
            val cached = ref
            if (cached !== Unset) return cached

            val clean = manager.getClean(this.field, id)
            refUpdater().lazySet(this, clean)
            return clean
        }

    override fun setValue(transaction: TRANSACTION, value: T) {
        if (field !is FieldDef.Mutable) throw UnsupportedOperationException()
        val manager = requireManaged()

        val _ref = ref
        val clean = if (_ref === Unset) manager.getClean(field, id) else Unset
        // after mutating dirty state we won't be able to see the clean one,
        // so we'll preserve it later in this method

        manager.set(transaction, field, id, if (clean === Unset) _ref else clean as T, value)
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

    private fun requireManaged(): Manager<SCH, TRANSACTION, ID> =
            manager ?: throw IllegalStateException(
                    "${toString()} is not managed anymore" + // e. g. was removed from underlying storage
                            if (ref !== Unset) ". Last remembered value: '$ref'" else "")

    override fun toString(): String =
            "ManagedProperty(at $field)"

}


/**
 * A manager of a property, e. g. a database DAO/session.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface Manager<SCH : Schema<SCH>, TRANSACTION, ID> {

    /**
     * Returns dirty transaction value for current thread, or [Unset], if none.
     */
    fun <T> getDirty(field: FieldDef.Mutable<SCH, T>, id: ID): T

    /**
     * Returns clean, persisted, stable, committed value visible for all threads.
     */
    fun <T> getClean(field: FieldDef<SCH, T>, id: ID): T

    /**
     * Sets 'dirty' value during [transaction].
     */
    fun <T> set(transaction: TRANSACTION, field: FieldDef.Mutable<SCH, T>, id: ID, previous: T, update: T)

}
