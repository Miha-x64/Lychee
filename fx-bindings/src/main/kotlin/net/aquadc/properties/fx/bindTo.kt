package net.aquadc.properties.fx

import net.aquadc.properties.Property
import javafx.beans.property.Property as FxProperty

fun <T> FxProperty<T>.bindTo(that: Property<T>) {
    this.value = that.value
    that.addChangeListener { _, new ->
        this.value = new
    }
}
