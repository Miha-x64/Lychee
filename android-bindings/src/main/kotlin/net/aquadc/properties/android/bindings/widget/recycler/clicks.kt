@file:JvmName("RecyclerViewClicks")
@file:Suppress("NOTHING_TO_INLINE")
package net.aquadc.properties.android.bindings.widget.recycler

import android.view.View
import net.aquadc.properties.MutableProperty


/**
 * Returns a [View.OnClickListener] which sets [this] [Holder.item] to [target] when triggered.
 */
inline fun <T> Holder<T>.setItemWhenClicked(target: MutableProperty<in T>): View.OnClickListener =
        ClickListener(this, target)


/**
 * Returns a [View.OnLongClickListener] which sets [this] [Holder.item] to [target] when triggered.
 */
inline fun <T> Holder<T>.setItemWhenLongClicked(target: MutableProperty<in T>): View.OnLongClickListener =
        ClickListener(this, target)


@Suppress("UNCHECKED_CAST")
@PublishedApi internal class ClickListener<T>(
        private val holder: Any,
        private val itemTarget: MutableProperty<in T>
) : View.OnClickListener, View.OnLongClickListener {

    override fun onClick(ignored: View?) {
        itemTarget.value = (holder as Holder<T>).item
    }

    override fun onLongClick(ignored: View?): Boolean {
        onClick(null)
        return true
    }

}
