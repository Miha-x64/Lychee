package net.aquadc.properties.android.bindings

import android.os.Build
import android.support.annotation.RequiresApi
import android.view.View
import net.aquadc.properties.Property


/**
 * Binds view to [source] property via [bind] function.
 *
 * For properties which [Property.mayChange],
 * calls [bind] for each [source] change while [View.isAttachedToWindow],
 * and on [View.OnAttachStateChangeListener.onViewAttachedToWindow].
 *
 * For immutable properties, calls [bind] in-place.
 */
fun <V : View, T> V.bindViewTo(source: Property<T>, bind: (view: V, new: T) -> Unit) {
    if (source.mayChange) {
        val binding = SafeBinding(this, source, bind)
        if (windowToken != null) {
            // the view is already attached, catch up with this state
            binding.onViewAttachedToWindow(this)
        }
        addOnAttachStateChangeListener(binding)
    } else {
        bind(this, source.value)
    }
}


/**
 * Binds view [destination] property to [source] property.
 *
 * For properties which [Property.mayChange],
 * [android.util.Property.set]s new value for each [source] change while [View.isAttachedToWindow],
 * and on [View.OnAttachStateChangeListener.onViewAttachedToWindow].
 *
 * For immutable properties, calls [android.util.Property.set] in-place.
 */
@RequiresApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
fun <V : View, T> V.bindViewTo(source: Property<T>, destination: android.util.Property<V, T>) =
        bindViewTo(source) { obj, value -> destination.set(obj, value) }


private class SafeBinding<V : View, in T>(
        private val view: V,
        private val property: Property<T>,
        private val bind: (V, T) -> Unit
) : View.OnAttachStateChangeListener, (T, T) -> Unit {

    override fun invoke(p1: T, p2: T) {
        bind(view, p2)
    }

    override fun onViewAttachedToWindow(v: View) {
        // We're probably the first listener,
        // subscription may trigger value computation.
        property.addChangeListener(this)

        // Bind when value computed.
        bind(view, property.value)
    }

    override fun onViewDetachedFromWindow(v: View) {
        property.removeChangeListener(this)
    }

}

// Note: README.md contains links to this file with line numbers of bindViewTo(Property, function) and SafeBinding.
