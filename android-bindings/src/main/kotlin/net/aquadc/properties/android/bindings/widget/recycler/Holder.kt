package net.aquadc.properties.android.bindings.widget.recycler

import androidx.recyclerview.widget.RecyclerView

/**
 * A simple mutable holder of [T] object.
 * To be implemented by [RecyclerView.ViewHolder]
 */
interface Holder<T> {

    /**
     * Current item.
     * May throw [NoSuchElementException] if not bound yet or already unbound.
     */
    val item: T

}
