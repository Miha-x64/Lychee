package net.aquadc.properties.android

import android.view.View
import net.aquadc.properties.Property

fun View.bindEnabledTo(enabledProperty: Property<Boolean>) {
    isEnabled = enabledProperty.value
    enabledProperty.addChangeListener { _, new -> isEnabled = new }
}
