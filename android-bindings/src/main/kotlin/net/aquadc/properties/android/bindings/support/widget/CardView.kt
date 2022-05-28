@file:JvmName("CardViewBindings")
package net.aquadc.properties.android.bindings.support.widget

import android.support.v4.content.ContextCompat
import android.support.v7.widget.CardView
import net.aquadc.properties.Property


/**
 * Binds background color using [CardView.setBackgroundColor].
 */
@Deprecated(
    "Bindings to Android Support library are abandoned. Consider migrating to AndroidX.",
    ReplaceWith("this.bindCardBackgroundColorTo(backgroundColorProperty)", "net.aquadc.properties.android.bindings.androidx.widget.bindCardBackgroundColorTo"),
    DeprecationLevel.ERROR
)
fun CardView.bindCardBackgroundColorTo(backgroundColorProperty: Property<Int>): Unit =
    throw AssertionError()

/**
 * Binds background color resource using [CardView.setBackgroundColor] and [ContextCompat.getColor].
 */
@Deprecated(
    "Bindings to Android Support library are abandoned. Consider migrating to AndroidX.",
    ReplaceWith("this.bindCardBackgroundColorResTo(backgroundColorResProperty)", "net.aquadc.properties.android.bindings.androidx.widget.bindCardBackgroundColorResTo"),
    DeprecationLevel.ERROR
)
fun CardView.bindCardBackgroundColorResTo(backgroundColorResProperty: Property<Int>): Unit =
    throw AssertionError()
