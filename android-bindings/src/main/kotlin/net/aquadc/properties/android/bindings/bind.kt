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
fun <V : View, T> V.bindViewTo(property: Property<T>, bind: (view: V, new: T) -> Unit) {
    if (Build.VERSION.SDK_INT >= 19 && isAttachedToWindow) {
        throw IllegalStateException(
                "Must bind view before it gets attached. Use onCreateView instead of onViewCreated in Fragments."
        )
    }

    addOnAttachStateChangeListener(SafeBinding(this, property, bind))
}

/**
 * Calls specified [bind] function when
 * * View gets attached to window
 * * [property]'s value gets changed while View is attached
 */
class SafeBinding<V : View, in T>(
        private val view: V,
        private val property: Property<T>,
        private val bind: (V, T) -> Unit
) : View.OnAttachStateChangeListener, (T, T) -> Unit {

    override fun invoke(p1: T, p2: T) {
        bind(view, p2)
    }

    override fun onViewAttachedToWindow(v: View) {
        bind(view, property.value)
        property.addChangeListener(this)
    }

    override fun onViewDetachedFromWindow(v: View) {
        property.removeChangeListener(this)
    }

}
