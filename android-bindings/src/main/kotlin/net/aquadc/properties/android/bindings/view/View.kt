@file:JvmName("ViewBindings")
@file:Suppress("NOTHING_TO_INLINE")
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
inline fun View.bindVisibilitySoftlyTo(visibleProperty: Property<Boolean>): Unit =
        bindVisibilityTo(visibleProperty, View.INVISIBLE)

/**
 * Passes [visibleProperty] value to [View.setVisibility]:
 * `true` means [View.VISIBLE], `false` means [View.GONE].
 */
inline fun View.bindVisibilityHardlyTo(visibleProperty: Property<Boolean>): Unit =
        bindVisibilityTo(visibleProperty, View.GONE)

@PublishedApi @JvmSynthetic internal fun View.bindVisibilityTo(visibleProperty: Property<Boolean>, invisible: Int): Unit =
        bindViewTo(visibleProperty) { view, visible ->
            view.visibility = if (visible) View.VISIBLE else invisible
        }

/**
 * Passes [enabledProperty] value to [View.setEnabled].
 */
fun View.bindEnabledTo(enabledProperty: Property<Boolean>): Unit =
        bindViewTo(enabledProperty) { v, ena -> v.isEnabled = ena }

/**
 * Binds background using [View.setBackground].
 */
inline fun View.bindBackgroundTo(backgroundProperty: Property<Drawable?>): Unit =
        bindBackgroundTo(backgroundProperty, false)

/**
 * Binds background using [View.setBackgroundResource].
 */
inline fun View.bindBackgroundResTo(backgroundProperty: Property<Int>): Unit =
        bindBackgroundTo(backgroundProperty, false)

/**
 * Binds background color using [View.setBackgroundColor].
 */
inline fun View.bindBackgroundColorTo(backgroundColorProperty: Property<Int>): Unit =
        bindBackgroundTo(backgroundColorProperty, true)

@PublishedApi @JvmSynthetic internal fun View.bindBackgroundTo(backgroundProperty: Property<*/* Drawable | Int | null */>, color: Boolean) =
        bindViewTo(backgroundProperty) { view, back ->
            @Suppress("DEPRECATION")
            when (back) {
                is Drawable? ->
                    if (Build.VERSION.SDK_INT >= 16) view.background = back else view.setBackgroundDrawable(back)
                is Int ->
                    if (color) view.setBackgroundColor(back) else view.setBackgroundResource(back)
                else ->
                    throw AssertionError()
            }
        }

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
