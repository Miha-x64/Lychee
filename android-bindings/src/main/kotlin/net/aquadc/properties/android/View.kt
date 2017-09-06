package net.aquadc.properties.android

import android.view.View
import net.aquadc.properties.MutableProperty
import net.aquadc.properties.mutablePropertyOf

fun View.enabledProperty(): MutableProperty<Boolean> {
    val prop = mutablePropertyOf(isEnabled)
    prop.addChangeListener { _, new -> isEnabled = new }
    return prop
}