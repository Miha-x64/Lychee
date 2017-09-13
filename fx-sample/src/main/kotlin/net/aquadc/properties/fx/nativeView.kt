package net.aquadc.properties.fx

import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXTextField
import javafx.geometry.Insets
import javafx.scene.layout.VBox

fun nativeView(presenter: Presenter) = VBox(10.0).apply {

    padding = Insets(10.0, 10.0, 10.0, 10.0)

    children.add(JFXTextField().apply {
        promptText = "Email"
        textProperty().bindBidirectional(presenter.emailProp)
    })
    children.add(JFXTextField().apply {
        promptText = "Name"
        textProperty().bindBidirectional(presenter.nameProp)
    })
    children.add(JFXTextField().apply {
        promptText = "Surname"
        textProperty().bindBidirectional(presenter.surnameProp)
    })
    children.add(JFXButton("Press me, hey, you!").apply {
        styleClass.add("button-raised")
        disableProperty().bind(!presenter.buttonEnabledProp)
        textProperty().bind(presenter.buttonTextProp)
        setOnAction { presenter.saveButtonClicked() }
    })

}
