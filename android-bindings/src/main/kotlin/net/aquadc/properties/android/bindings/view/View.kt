package net.aquadc.properties.android.bindings.view

import android.annotation.TargetApi
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import net.aquadc.properties.MutableProperty
import net.aquadc.properties.Property
import net.aquadc.properties.android.bindings.bindViewTo
import net.aquadc.properties.clear
import net.aquadc.properties.set
import java.lang.AssertionError


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
 * Binds background using [View.setBackground].
 */
fun View.bindBackgroundTo(backgroundProperty: Property<Drawable?>) =
        bindViewTo(backgroundProperty, SetBackground.Drawable)

/**
 * Binds background using [View.setBackgroundResource].
 */
@JvmName("bindBackgroundResourceTo")
fun View.bindBackgroundTo(backgroundProperty: Property<Int>) =
        bindViewTo(backgroundProperty, SetBackground.Drawable)

/**
 * Binds background color using [View.setBackgroundColor].
 */
fun View.bindBackgroundColorTo(backgroundColorProperty: Property<Int>) =
        bindViewTo(backgroundColorProperty, SetBackground.Color)

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
