package net.aquadc.properties.android.bindings.support.widget.recycler

import android.support.v7.widget.RecyclerView
import androidx.annotation.CallSuper

/**
 * A simple abstract [RecyclerView.Adapter] which knows whether it is being observed by [RecyclerView] instances.
 */
@Deprecated(
    "Bindings to Android Support library are abandoned. Consider migrating to AndroidX.",
    ReplaceWith("ObservingAdapter", "net.aquadc.properties.android.bindings.androidx.widget.recycler.ObservingAdapter"),
    DeprecationLevel.ERROR
)
abstract class ObservingAdapter<VH : RecyclerView.ViewHolder> : RecyclerView.Adapter<VH>() {

    /**
     * Called to notify that this adapter observed or not observed now.
     */
    protected abstract fun onObservedStateChanged(observed: Boolean)

}
