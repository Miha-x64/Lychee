package net.aquadc.properties.android.bindings

import android.view.View
import net.aquadc.properties.Property


/**
 * Creates new [SafeBinding] and makes it observe [this] View's attached state.
 */
fun <V : View, T> V.bindViewTo(property: Property<T>, bind: (view: V, new: T) -> Unit) {
    val binding = SafeBinding(this, property, bind)
    if (windowToken != null) {
        // the view is already attached, catch up with this state
        binding.onViewAttachedToWindow(this)
    }
    addOnAttachStateChangeListener(binding)
}

/**
 * Calls specified [bind] function when
 * * View gets attached to window
 * * [property]'s value gets changed while View is attached
 */
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
