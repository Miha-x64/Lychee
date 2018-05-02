package net.aquadc.properties.fx

import net.aquadc.properties.MutableProperty
import javafx.beans.property.Property as FxProperty

/**
 * Binds [this] and [that] property values.
 * [this] takes [that] value first.
 */
fun <T> FxProperty<T>.bindBidirectionally(that: MutableProperty<T>) {
    this.value = that.value
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
