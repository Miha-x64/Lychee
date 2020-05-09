@file:JvmName("LabeledBindings")
package net.aquadc.properties.fx

import javafx.scene.control.Labeled
import net.aquadc.properties.Property

fun Labeled.bindTextTo(textProperty: Property<String>) {
    textProperty().bind(textProperty.fx())
}
