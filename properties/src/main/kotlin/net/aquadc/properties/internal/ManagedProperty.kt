package net.aquadc.properties.internal

import android.support.annotation.RestrictTo
import net.aquadc.properties.TransactionalProperty
import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.StructDef

/**
 * A property whose value can be changed inside a transaction.
 *
 * Note: [manager] is not volatile and requires external synchronization if used (e. g. [dropManagement]) concurrently
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ManagedProperty<DEF : StructDef<DEF>, TRANSACTION, T> constructor(
        private var manager: Manager<DEF, TRANSACTION>?,
        private val field: FieldDef.Mutable<DEF, T>,
        private val id: Long, // TODO: version without this field
        initialValue: T
) : `Notifier-1AtomicRef`<T, T>(true, initialValue), TransactionalProperty<TRANSACTION, T> {

    override val value: T
        get() {
            val manager = requireManaged()

            // check for uncommitted changes
            val dirty = manager.getDirty(this.field, id)
            if (dirty !== Unset) return dirty

            // check cached
            val cached = ref
            if (cached !== Unset) return cached

            val clean = manager.getClean(this.field, id)
            refUpdater().lazySet(this, clean)
            return clean
        }

    override fun setValue(transaction: TRANSACTION, value: T) {
        val manager = requireManaged()

        val clean = if (ref === Unset) manager.getClean(field, id) else Unset
        // after mutating dirty state we won't be able to see the clean one,
        // so we'll preserve it later in this method

        manager.set(transaction, field, id, value)
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

    private fun requireManaged(): Manager<DEF, TRANSACTION> =
            manager ?: throw IllegalStateException(
                    "${toString()} is not managed anymore, e. g. was removed from underlying storage" +
                            if (ref !== Unset) ". Last remembered value: '$ref'" else "")

    override fun toString(): String =
            "ManagedProperty(at $field#$id)"

}

/**
 * A manager of a property, e. g. a database session.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
abstract class Manager<DEF : StructDef<DEF>, TRANSACTION> {

    /**
     * Returns dirty transaction value for current thread, or [Unset], if none.
     */
    @Suppress("UNCHECKED_CAST")
    open fun <T> getDirty(field: FieldDef.Mutable<DEF, T>, id: Long): T =
            Unset as T

    /**
     * Returns clean value.
     */
    abstract fun <T> getClean(field: FieldDef.Mutable<DEF, T>, id: Long): T

    /**
     * Sets 'dirty' value during [transaction].
     */
    abstract fun <T> set(transaction: TRANSACTION, field: FieldDef.Mutable<DEF, T>, id: Long, update: T)

}
