package net.aquadc.properties.android.bindings.androidx.widget.recycler

import androidx.annotation.CallSuper
import androidx.recyclerview.widget.RecyclerView

/**
 * A simple abstract [RecyclerView.Adapter] which knows whether it is being observed by [RecyclerView] instances.
 * Don't forget to null out the adapter! Keep an eye on memory leaks! https://issuetracker.google.com/issues/154751401
 * @see RecyclerView.observeAdapter
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
