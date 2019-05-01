package net.aquadc.properties.internal

import android.support.annotation.RestrictTo
import net.aquadc.properties.TransactionalProperty
import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.NamedLens
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct

/**
 * A property whose value can be changed inside a transaction.
 *
 * Note: [manager] is not volatile and requires external synchronization if used (e. g. [dropManagement]) concurrently
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
open class ManagedProperty<SCH : Schema<SCH>, TRANSACTION, T, ID>(
        private var manager: Manager<SCH, TRANSACTION, ID>?,
        private val column: NamedLens<SCH, Struct<SCH>, T>,
        private val id: ID,
        initialValue: T
) : `Notifier-1AtomicRef`<T, T>(true, initialValue), TransactionalProperty<TRANSACTION, T> {

    override val value: T
        get() {
            val manager = requireManaged()

            // check for uncommitted changes
            val dirty = manager.getDirty(this.column, id)
            if (dirty !== Unset) return dirty

            // check cached
            val cached = ref
            if (cached !== Unset) return cached

            val clean = manager.getClean(this.column, id)
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

    private fun requireManaged(): Manager<SCH, TRANSACTION, ID> =
            manager ?: throw IllegalStateException(
                    "${toString()} is not managed anymore, e. g. was removed from underlying storage" +
                            if (ref !== Unset) ". Last remembered value: '$ref'" else "")

    override fun toString(): String =
            "ManagedProperty(at $column)"

}


/**
 * A manager of a property, e. g. a database DAO/session.
 * [FieldDef]s of embedded structs may belong to other [Schema], or belong to the same one and interfere with main ones.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface Manager<SCH : Schema<SCH>, TRANSACTION, ID> {

    /**
     * Returns dirty transaction value for current thread, or [Unset], if none.
     */
    fun <T> getDirty(column: NamedLens<SCH, Struct<SCH>, T>, id: ID): T

    /**
     * Returns clean, persisted, stable, committed value visible for all threads.
     */
    fun <T> getClean(column: NamedLens<SCH, Struct<SCH>, T>, id: ID): T

    /**
     * Sets 'dirty' value during [transaction].
     */
    fun <T> set(transaction: TRANSACTION, column: NamedLens<SCH, Struct<SCH>, T>, id: ID, update: T)

}
