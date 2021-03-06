@file:JvmName("CardViewBindings")
package net.aquadc.properties.android.bindings.androidx.widget

import androidx.core.content.ContextCompat
import androidx.cardview.widget.CardView
import net.aquadc.properties.Property
import net.aquadc.properties.android.bindings.bindViewTo


/**
 * Binds background color using [CardView.setBackgroundColor].
 */
fun CardView.bindCardBackgroundColorTo(backgroundColorProperty: Property<Int>): Unit =
        bindViewTo(backgroundColorProperty) { cv, color -> cv.setCardBackgroundColor(color) }

/**
 * Binds background color resource using [CardView.setBackgroundColor] and [ContextCompat.getColor].
 */
fun CardView.bindCardBackgroundColorResTo(backgroundColorResProperty: Property<Int>): Unit =
        bindViewTo(backgroundColorResProperty) { cv, colorRes ->
            cv.setCardBackgroundColor(ContextCompat.getColor(cv.context, colorRes))
        }
