package net.aquadc.properties.fx

import net.aquadc.properties.MutableProperty
import net.aquadc.properties.value
import javafx.beans.property.Property as FxProperty

fun <T> FxProperty<T>.bindBidirectionally(that: MutableProperty<T>) {
    value = that.getValue()
    var mutatingThis = false
    var mutatingThat = false

    this.addListener { _, _, new ->
        if (!mutatingThis) {
            mutatingThat = true
            that.value = new
            mutatingThat = false
        }
    }

    that.addChangeListener { _, new ->
        if (!mutatingThat) {
            mutatingThis = true
            this.value = new
            mutatingThis = false
        }
    }
}
