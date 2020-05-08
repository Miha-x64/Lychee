package net.aquadc.properties.fx

import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXTextField
import javafx.geometry.Insets
import javafx.scene.layout.VBox


fun nativeView(viewModel: FxViewModel) = VBox(10.0).apply {

    padding = Insets(10.0, 10.0, 10.0, 10.0)

    children.add(JFXTextField().apply {
        promptText = "Email"
        textProperty().bindBidirectional(viewModel.emailProp)
    })

    children.add(JFXTextField().apply {
        promptText = "Name"
        textProperty().bindBidirectional(viewModel.nameProp)
    })

    children.add(JFXTextField().apply {
        promptText = "Surname"
        textProperty().bindBidirectional(viewModel.surnameProp)
    })

    children.add(JFXButton("Press me, hey, you!").apply {
        disableProperty().bind(!viewModel.buttonEnabledProp)
        textProperty().bind(viewModel.buttonTextProp)
        setOnAction { viewModel.saveButtonClicked() }
    })

}
