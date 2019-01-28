package net.aquadc.properties.fx

import javafx.scene.control.ButtonBase
import net.aquadc.properties.MutableProperty
import net.aquadc.properties.set

/**
 * Sets [clickedProperty] value to `true` when [this] node is clicked.
 */
fun ButtonBase.setWhenClicked(clickedProperty: MutableProperty<Boolean>) {
    setOnAction { clickedProperty.set() }
}
