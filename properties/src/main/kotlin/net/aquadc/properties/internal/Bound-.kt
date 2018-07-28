package net.aquadc.properties.internal

import net.aquadc.properties.*
import net.aquadc.properties.executor.InPlaceWorker

/**
 * Bidirectional mapping.
 */
@PublishedApi internal class `Bound-`<T, R>(
        original: MutableProperty<T>,
        fwd: (T) -> R,
        private val bk: (R) -> T
) : `Mapped-`<T, R>(original, fwd, InPlaceWorker), MutableProperty<R>, ChangeListener<R> {

    override var value: R
        get() = super.value
        set(value) {
            (original as MutableProperty<T>).value = bk(value)
        }

    override fun bindTo(sample: Property<R>) {
        (original as MutableProperty<T>).bindTo(sample.map(bk))
    }

    override fun casValue(expect: R, update: R): Boolean =
            (original as MutableProperty<T>).casValue(bk(expect), bk(update))

    override fun invoke(old: R, new: R) {
        value = new
    }

}
