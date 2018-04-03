package net.aquadc.properties.android.bindings

import android.os.Build
import android.view.View
import net.aquadc.properties.Property


/**
 * Creates new [SafeBinding] and makes it observe [this] View's attached state.
 * View must be detached from window
 * because I don't know any safe ways to check whether it is attached
 * and requires instantaneous binding and subscription or not.
 */
fun <T> View.bindViewTo(property: Property<T>, bind: (new: T) -> Unit) {
    if (Build.VERSION.SDK_INT >= 19 && isAttachedToWindow) {
        throw IllegalStateException("Must bind view before it gets attached. Use onCreateView in Fragments.")
    }

    addOnAttachStateChangeListener(SafeBinding(property, bind))
}

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
        bind(property.value)
        property.addChangeListener(listener)
    }

    override fun onViewDetachedFromWindow(v: View) {
        property.removeChangeListener(listener)
    }

}
