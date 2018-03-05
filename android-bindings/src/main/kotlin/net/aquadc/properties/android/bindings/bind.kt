package net.aquadc.properties.android.bindings

import android.view.View
import net.aquadc.properties.Property


/**
 * Creates new [SafeBinding] and makes it observe [this] View's attached state.
 */
fun <T> View.bindViewTo(property: Property<T>, bind: (new: T) -> Unit) =
        addOnAttachStateChangeListener(SafeBinding(property, bind))

/**
 * Calls specified [bind] function when
 * * View gets attached to window
 * * [property]'s value gets changed while View is attached
 */
class SafeBinding<T>(
        private val property: Property<T>,
        private val bind: (T) -> Unit
) : View.OnAttachStateChangeListener {

    private val listener = { _: T, new: T -> bind(new) }

    override fun onViewAttachedToWindow(v: View) {
        bind(property.getValue())
        property.addChangeListener(listener)
    }

    override fun onViewDetachedFromWindow(v: View) {
        property.removeChangeListener(listener)
    }

}
