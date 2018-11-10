package net.aquadc.properties.android.bindings.widget.recycler

import android.support.annotation.CallSuper
import android.support.v7.widget.RecyclerView

/**
 * A simple abstract [RecyclerView.Adapter] which knows whether it is being observed by [RecyclerView] instances.
 */
abstract class ObservingAdapter<VH : RecyclerView.ViewHolder> : RecyclerView.Adapter<VH>() {

    private var recyclers = 0

    @CallSuper
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        if (recyclers++ == 0) onObservedStateChanged(true)
    }

    @CallSuper
    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        if (--recyclers == 0) onObservedStateChanged(false)
        super.onDetachedFromRecyclerView(recyclerView)
    }

    /**
     * Called to notify that this adapter observed or not observed now.
     */
    protected abstract fun onObservedStateChanged(observed: Boolean)

}
