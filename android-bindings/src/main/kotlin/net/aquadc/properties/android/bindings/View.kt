package net.aquadc.properties.android.bindings

import android.annotation.TargetApi
import android.view.View
import net.aquadc.properties.MutableProperty
import net.aquadc.properties.Property
import net.aquadc.properties.clear
import net.aquadc.properties.set


/**
 * Passes [visibleProperty] value to [View.setVisibility]:
 * `true` means [View.VISIBLE], `false` means [View.INVISIBLE].
 */
fun View.bindVisibilitySoftlyTo(visibleProperty: Property<Boolean>) =
        bindViewTo(visibleProperty, SetVisibilitySoftly)

/**
 * Passes [visibleProperty] value to [View.setVisibility]:
 * `true` means [View.VISIBLE], `false` means [View.GONE].
 */
fun View.bindVisibilityHardlyTo(visibleProperty: Property<Boolean>) =
        bindViewTo(visibleProperty, SetVisibilityHardly)

/**
 * Passes [enabledProperty] value to [View.setEnabled].
 */
fun View.bindEnabledTo(enabledProperty: Property<Boolean>) =
        bindViewTo(enabledProperty) { v, ena -> v.isEnabled = ena }

/**
 * Sets [clickedProperty] to `true` when [this] view gets clicked.
 */
fun View.setWhenClicked(clickedProperty: MutableProperty<Boolean>) =
        setOnClickListener { clickedProperty.set() }

/**
 * Observes [View.isAttachedToWindow] value.
 * [android.view.View.addOnAttachStateChangeListener] requires minSdk 12,
 * but [android.view.View.isAttachedToWindow] requires minSdk 19.
 */
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
