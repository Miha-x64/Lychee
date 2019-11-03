@file:JvmName("RecyclerViewHolderClicks")
@file:Suppress("NOTHING_TO_INLINE")
package net.aquadc.properties.android.bindings.support.widget.recycler

import android.support.v7.widget.RecyclerView
import android.view.View
import net.aquadc.properties.MutableProperty


/**
 * Returns a [View.OnClickListener] which sets [this] [RecyclerView.ViewHolder.getLayoutPosition] to [target] when triggered.
 */
inline fun RecyclerView.ViewHolder.setPositionWhenClicked(target: MutableProperty<in Int>): View.OnClickListener =
        ClickListener<Nothing>(this, target)


/**
 * Returns a [View.OnLongClickListener] which sets [this] [RecyclerView.ViewHolder.getLayoutPosition] to [target] when triggered.
 */
inline fun RecyclerView.ViewHolder.setPositionWhenLongClicked(target: MutableProperty<in Int>): View.OnLongClickListener =
        ClickListener<Nothing>(this, target)


@Suppress("UNCHECKED_CAST")
@PublishedApi internal class ClickListener<T>(
        private val holder: Any,
        private val positionTarget: MutableProperty<in Int>
) : View.OnClickListener, View.OnLongClickListener {

    override fun onClick(ignored: View?) {
        positionTarget.value = (holder as RecyclerView.ViewHolder).layoutPosition
    }

    override fun onLongClick(ignored: View?): Boolean {
        onClick(null)
        return true
    }

}
