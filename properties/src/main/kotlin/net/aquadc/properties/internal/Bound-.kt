package net.aquadc.properties.internal

import net.aquadc.properties.*
import net.aquadc.properties.executor.InPlaceWorker

/**
 * Bidirectional mapping.
 */
@PublishedApi
internal class `Bound-`<TRANSACTION, T, R>(
        original: Property<T>, // = MutableProperty | TransactionalProperty
        map: TwoWay<T, R>
) : `Mapped-`<T, R>(original, map, InPlaceWorker), MutableProperty<R>, TransactionalProperty<TRANSACTION, R>, ChangeListener<R> {

    override var value: R
        get() = super.value
        set(value) {
            mOrig.value = mapping.backwards(value)
        }

    override fun setValue(transaction: TRANSACTION, value: R) {
        tOrig.setValue(transaction, mapping.backwards(value))
    }

    override fun bindTo(sample: Property<R>) {
        mOrig.bindTo(sample.map(mapping::backwards))
        //                             ^^
        // extra allocation, but this is an extremely rare case
    }

    override fun casValue(expect: R, update: R): Boolean =
            mOrig.casValue(mapping.backwards(expect), mapping.backwards(update))

    override fun invoke(old: R, new: R) {
        value = new
    }

    private inline val mOrig get() = original as MutableProperty<T>
    private inline val tOrig get() = original as TransactionalProperty<TRANSACTION, T>
    private inline val mapping get() = map as TwoWay<T, R>

    /**
     * Represents a function which can be un-applied.
     * For example, when `invoke(arg) = 10 * arg`, `backwards(arg) = arg / 10`.
     */
    interface TwoWay<T, R> : (T) -> R {

        /**
         * Represents an action opposite to invoking this function.
         */
        fun backwards(arg: R): T
    }

}
