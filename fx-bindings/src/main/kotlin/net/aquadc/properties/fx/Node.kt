@file:JvmName("NodeBindings")
package net.aquadc.properties.fx

import javafx.scene.Node
import net.aquadc.properties.Property
import net.aquadc.properties.not

/**
 * Binds [Node.visibleProperty] to [visibleProperty].
 */
fun Node.bindVisibilitySoftlyTo(visibleProperty: Property<Boolean>) {
    visibleProperty().bind(visibleProperty.fx())
}

/**
 * Binds [Node.visibleProperty] and [Node.managedProperty] to [visibleProperty].
 */
fun Node.bindVisibilityHardlyTo(visibleProperty: Property<Boolean>) {
    // As suggested in https://stackoverflow.com/a/28559958/3050249
    managedProperty().bind(visibleProperty.fx())
    visibleProperty().bind(visibleProperty.fx())
}

/**
 * Binds [Node.disableProperty] to ![enableProperty].
 */
fun Node.bindEnableTo(enableProperty: Property<Boolean>) {
    disableProperty().bind(enableProperty.not().fx())
}
