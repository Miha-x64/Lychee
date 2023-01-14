@file:JvmName("RecyclerViewBindings")
package net.aquadc.properties.android.bindings.androidx.widget.recycler

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import net.aquadc.properties.android.R

/**
 * Calls [RecyclerView.swapAdapter] (adapter, false) respecting attached state:
 * when this [RecyclerView] gets detached, `swapAdapter(null, false)` is called.
 * If adapter observes some [net.aquadc.properties.Property] instance(s),
 * this may help unobserving properties (but not unreferencing them).
 * @see ObservingAdapter
 */
fun RecyclerView.observeAdapter(adapter: RecyclerView.Adapter<*>?) {
    val attacher = getTag(R.id._lychee_adapterAttacher)
    if (attacher == null)
        AdapterAttacher(adapter).also {
            setTag(R.id._lychee_adapterAttacher, it)
            addOnAttachStateChangeListener(it)
        }
    else
        (attacher as AdapterAttacher).adapter = adapter

    if (isAttachedToWindow /*this is overridden in RecyclerView, no minSdk problem*/)
        swapAdapter(adapter, false)
}

val RecyclerView.observedAdapter: RecyclerView.Adapter<*>?
    get() = (getTag(R.id._lychee_adapterAttacher) as AdapterAttacher?)?.adapter

private class AdapterAttacher(
    @JvmField var adapter: RecyclerView.Adapter<*>?,
) : View.OnAttachStateChangeListener {
    override fun onViewAttachedToWindow(p0: View?) {
        (p0 as RecyclerView).swapAdapter(adapter, false)
    }
    override fun onViewDetachedFromWindow(p0: View?) {
        (p0 as RecyclerView).swapAdapter(null, false)
    }
}
