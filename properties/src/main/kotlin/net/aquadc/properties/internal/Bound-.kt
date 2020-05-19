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

    @Suppress("UNCHECKED_CAST")
    override fun setValue(transaction: TRANSACTION, value: R) =
        (original as TransactionalProperty<TRANSACTION, T>)
            .setValue(transaction, mapping.backwards(value))

    override fun bindTo(sample: Property<R>) {
        mOrig.bindTo(sample.map(Backwards(mapping)))
        //                             ^^
        // extra allocation, but this is an extremely rare case
    }

    override fun casValue(expect: R, update: R): Boolean =
            mOrig.casValue(mapping.backwards(expect), mapping.backwards(update))

    override fun invoke(old: R, new: R) {
        value = new
    }

    private inline val mOrig get() = original as MutableProperty<T>
    private inline val mapping get() = map as TwoWay<T, R>

    private class Backwards<T, R>(
            private val twoWay: TwoWay<T, R>
    ) : (R) -> T {
        override fun invoke(p1: R): T =
                twoWay.backwards(p1)
    }

}
