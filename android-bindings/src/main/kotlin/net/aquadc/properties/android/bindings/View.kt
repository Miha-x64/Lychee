package net.aquadc.properties.android.bindings

import android.annotation.TargetApi
import android.view.View
import net.aquadc.properties.MutableProperty
import net.aquadc.properties.Property
import net.aquadc.properties.clear
import net.aquadc.properties.set


fun View.bindVisibilitySoftlyTo(visibleProperty: Property<Boolean>) =
        bindViewTo(visibleProperty, SetVisibilitySoftly)

fun View.bindVisibilityHardlyTo(visibleProperty: Property<Boolean>) =
        bindViewTo(visibleProperty, SetVisibilityHardly)

fun View.bindEnabledTo(enabledProperty: Property<Boolean>) =
        bindViewTo(enabledProperty) { v, ena -> v.isEnabled = ena }


fun View.setWhenClicked(clickedProperty: MutableProperty<Boolean>) =
        setOnClickListener { clickedProperty.set() }

@TargetApi(19)
fun View.bindToAttachedToWidow(attachedToWindowProperty: MutableProperty<Boolean>) {
    attachedToWindowProperty.value = isAttachedToWindow
    addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
            attachedToWindowProperty.set()
        }
        override fun onViewDetachedFromWindow(v: View?) {
            attachedToWindowProperty.clear()
        }
    })
}

private val SetVisibilitySoftly = SetVisibility(View.INVISIBLE)
private val SetVisibilityHardly = SetVisibility(View.GONE)

private class SetVisibility(
        private val invisible: Int
) : (View, Boolean) -> Unit {
    override fun invoke(p1: View, p2: Boolean) {
        p1.visibility = if (p2) View.VISIBLE else invisible
    }
}
