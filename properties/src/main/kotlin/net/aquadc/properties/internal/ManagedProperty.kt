package net.aquadc.properties.internal

import android.support.annotation.RestrictTo
import net.aquadc.properties.MutableProperty
import net.aquadc.properties.Property

/**
 * A property whose value can be changed inside a transaction.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ManagedProperty<T, TOKN>(
        private val manager: Manager<TOKN, T>,
        private val token: TOKN,
        private val id: Long
) : `Notifier+1AtomicRef`<T, T>(true, unset()), MutableProperty<T> {

    override var value: T
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
        set(value) {
            check(casValue(Unset as T, value))
        }

    /**
     * This doesn't work for such properties for consistency reasons:
     * (1) normal concurrent in-memory property can be mutated from any thread, it's not a problem,
     *     but when the property is bound to a database field, it can be mutated only in a transaction,
     *     making such binding dangerous and unpredictable;
     * (2) transactions should be short, and property binding looks like a long-term thing.
     */
    override fun bindTo(sample: Property<T>): Nothing {
        throw UnsupportedOperationException("This is possible to implement but looks very questionable.")
    }

    override fun casValue(expect: T, update: T): Boolean {
        val clean = if (ref === Unset) manager.getClean(token, id) else Unset
        // after mutating dirty state we won't be able to see the clean one, so preserve it

        val success = manager.set(token, id, expect, update)
        // this changes 'dirty' state (and value returned by 'get'),
        // but we don't want to deliver it until it becomes clean

        if (clean !== Unset) ref = clean as T // mutated successfully, preserve clean

        return success
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

interface Manager<TOKN, T> {

    /**
     * Returns dirty transaction value for current thread, or [Unset], if none.
     */
    fun getDirty(token: TOKN, id: Long): T

    /**
     * Returns clean value.
     */
    fun getClean(token: TOKN, id: Long): T

    /**
     * Set, if [expected] === [Unset]; CAS otherwise.
     * @return if write was successful; simple sets are always successful, even if current value is already equal to [update].
     */
    fun set(token: TOKN, id: Long, expected: Any?, update: T): Boolean
}
