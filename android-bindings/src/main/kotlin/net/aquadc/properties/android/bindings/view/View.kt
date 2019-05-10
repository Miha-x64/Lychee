@file:JvmName("ViewBindings")
package net.aquadc.properties.android.bindings.view

import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import net.aquadc.properties.MutableProperty
import net.aquadc.properties.Property
import net.aquadc.properties.android.bindings.SetWhenClicked
import net.aquadc.properties.android.bindings.bindViewTo


// region Property Bindings

/**
 * Passes [visibleProperty] value to [View.setVisibility]:
 * `true` means [View.VISIBLE], `false` means [View.INVISIBLE].
 */
fun View.bindVisibilitySoftlyTo(visibleProperty: Property<Boolean>): Unit =
        bindViewTo(visibleProperty, SetVisibilitySoftly)

/**
 * Passes [visibleProperty] value to [View.setVisibility]:
 * `true` means [View.VISIBLE], `false` means [View.GONE].
 */
fun View.bindVisibilityHardlyTo(visibleProperty: Property<Boolean>): Unit =
        bindViewTo(visibleProperty, SetVisibilityHardly)

/**
 * Passes [enabledProperty] value to [View.setEnabled].
 */
fun View.bindEnabledTo(enabledProperty: Property<Boolean>): Unit =
        bindViewTo(enabledProperty) { v, ena -> v.isEnabled = ena }

/**
 * Binds background using [View.setBackground].
 */
fun View.bindBackgroundTo(backgroundProperty: Property<Drawable?>): Unit =
        bindViewTo(backgroundProperty, SetBackground.Drawable)

/**
 * Binds background using [View.setBackgroundResource].
 */
@JvmName("bindBackgroundResourceTo")
fun View.bindBackgroundTo(backgroundProperty: Property<Int>): Unit =
        bindViewTo(backgroundProperty, SetBackground.Drawable)

/**
 * Binds background color using [View.setBackgroundColor].
 */
fun View.bindBackgroundColorTo(backgroundColorProperty: Property<Int>): Unit =
        bindViewTo(backgroundColorProperty, SetBackground.Color)

// endregion Property Bindings

// region Event Bindings

/**
 * Sets [clickedProperty] to `true` when [this] view gets clicked.
 */
fun View.setWhenClicked(clickedProperty: MutableProperty<Boolean>): Unit =
        setOnClickListener(SetWhenClicked(clickedProperty))

/**
 * Sets [clickedProperty] to `true` when [this] view gets long-clicked.
 */
fun View.setWhenLongClicked(clickedProperty: MutableProperty<Boolean>): Unit =
        setOnLongClickListener(SetWhenClicked(clickedProperty))

// endregion Event Bindings

private val SetVisibilitySoftly = SetVisibility(View.INVISIBLE)
private val SetVisibilityHardly = SetVisibility(View.GONE)

private class SetVisibility(
        private val invisible: Int
) : (View, Boolean) -> Unit {
    override fun invoke(p1: View, p2: Boolean) {
        p1.visibility = if (p2) View.VISIBLE else invisible
    }
}

private class SetBackground(
        private val color: Boolean
) : (View, Any?) -> Unit {

    @Suppress("DEPRECATION")
    override fun invoke(view: View, back: Any?) = when (back) {
        is Drawable? -> if (Build.VERSION.SDK_INT >= 16) view.background = back else view.setBackgroundDrawable(back)
        is Int -> if (color) view.setBackgroundColor(back) else view.setBackgroundResource(back)
        else -> throw AssertionError()
    }

    companion object {
        @JvmField val Drawable = SetBackground(false)
        @JvmField val Color = SetBackground(true)
    }

}
