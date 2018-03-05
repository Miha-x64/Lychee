package net.aquadc.properties.android.bindings

import android.annotation.TargetApi
import android.view.View
import net.aquadc.properties.MutableProperty
import net.aquadc.properties.Property
import net.aquadc.properties.clear
import net.aquadc.properties.set


fun View.bindVisibilitySoftlyTo(visibleProperty: Property<Boolean>) =
        bindViewTo(visibleProperty) {
            visibility = if (it) View.VISIBLE else View.INVISIBLE
        }

fun View.bindVisibilityHardlyTo(visibleProperty: Property<Boolean>) =
        bindViewTo(visibleProperty) {
            visibility = if (it) View.VISIBLE else View.GONE
        }

fun View.bindEnabledTo(enabledProperty: Property<Boolean>) =
        bindViewTo(enabledProperty, ::setEnabled)


fun View.setWhenClicked(clickedProperty: MutableProperty<Boolean>) =
        setOnClickListener { clickedProperty.set() }

@TargetApi(19)
fun View.bindToAttachedToWidow(attachedToWindowProperty: MutableProperty<Boolean>) {
    attachedToWindowProperty.setValue(isAttachedToWindow)
    addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
            attachedToWindowProperty.set()
        }
        override fun onViewDetachedFromWindow(v: View?) {
            attachedToWindowProperty.clear()
        }
    })
}
