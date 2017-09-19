package net.aquadc.properties.android

import android.annotation.TargetApi
import android.view.View
import net.aquadc.properties.MutableProperty
import net.aquadc.properties.Property

fun View.bindVisibilitySoftlyTo(visibleProperty: Property<Boolean>) { // todo: test
    visibility = if (visibleProperty.value) View.VISIBLE else View.INVISIBLE
    visibleProperty.addChangeListener { _, new ->
        visibility = if (new) View.VISIBLE else View.INVISIBLE
    }
}

fun View.bindVisibilityHardlyTo(visibleProperty: Property<Boolean>) { // todo: test
    visibility = if (visibleProperty.value) View.VISIBLE else View.GONE
    visibleProperty.addChangeListener { _, new ->
        visibility = if (new) View.VISIBLE else View.GONE
    }
}

fun View.bindEnabledTo(enabledProperty: Property<Boolean>) {
    isEnabled = enabledProperty.value
    enabledProperty.addChangeListener { _, new -> isEnabled = new }
}

@TargetApi(19)
fun View.bindToAttachedToWidow(attachedToWindowProperty: MutableProperty<Boolean>) { // todo: test
    attachedToWindowProperty.value = isAttachedToWindow
    addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
            attachedToWindowProperty.value = true
        }
        override fun onViewDetachedFromWindow(v: View?) {
            attachedToWindowProperty.value = false
        }
    })
}
