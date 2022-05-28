@file:JvmName("RecyclerViewHolderClicks")
@file:Suppress("NOTHING_TO_INLINE")
package net.aquadc.properties.android.bindings.support.widget.recycler

import android.support.v7.widget.RecyclerView
import android.view.View
import net.aquadc.properties.MutableProperty


/**
 * Returns a [View.OnClickListener] which sets [this] [RecyclerView.ViewHolder.getLayoutPosition] to [target] when triggered.
 */
@Deprecated(
    "Bindings to Android Support library are abandoned. Consider migrating to AndroidX.",
    ReplaceWith("this.setPositionWhenClicked(target)", "net.aquadc.properties.android.bindings.androidx.widget.recycler.setPositionWhenClicked"),
    DeprecationLevel.ERROR
)
inline fun RecyclerView.ViewHolder.setPositionWhenClicked(target: MutableProperty<in Int>): View.OnClickListener =
    throw AssertionError()


/**
 * Returns a [View.OnLongClickListener] which sets [this] [RecyclerView.ViewHolder.getLayoutPosition] to [target] when triggered.
 */
@Deprecated(
    "Bindings to Android Support library are abandoned. Consider migrating to AndroidX.",
    ReplaceWith("this.setPositionWhenLongClicked(target)", "net.aquadc.properties.android.bindings.androidx.widget.recycler.setPositionWhenLongClicked"),
    DeprecationLevel.ERROR
)
inline fun RecyclerView.ViewHolder.setPositionWhenLongClicked(target: MutableProperty<in Int>): View.OnLongClickListener =
    throw AssertionError()
