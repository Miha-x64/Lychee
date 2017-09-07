@file:Suppress("NOTHING_TO_INLINE")
package net.aquadc.properties.android

import android.app.Activity
import android.app.Fragment
import android.view.View
import net.aquadc.properties.unsynchronizedMutablePropertyOf

inline fun <T> Activity.mutablePropertyOf(value: T) =
        unsynchronizedMutablePropertyOf(value)

inline fun <T> Fragment.mutablePropertyOf(value: T) =
        unsynchronizedMutablePropertyOf(value)

inline fun <T> View.mutablePropertyOf(value: T) =
        unsynchronizedMutablePropertyOf(value)
