package net.aquadc.properties.fx

import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXTextField
import javafx.geometry.Insets
import javafx.scene.layout.VBox
import net.aquadc.properties.not
import net.aquadc.properties.sample.logic.MainPresenter

fun viewWithOurProps(presenter: MainPresenter) = VBox(10.0).apply {

    padding = Insets(10.0, 10.0, 10.0, 10.0)

    val uiBridge = presenter.ui

    children.add(JFXTextField().apply {
        promptText = "Email"
        textProperty().bindBidirectionally(uiBridge.emailProp)
    })
    children.add(JFXTextField().apply {
        promptText = "Name"
        textProperty().bindBidirectionally(uiBridge.nameProp)
    })
    children.add(JFXTextField().apply {
        promptText = "Surname"
        textProperty().bindBidirectionally(uiBridge.surnameProp)
    })
    children.add(JFXButton("Press me, hey, you!").apply {
        styleClass.add("button-raised")
        disableProperty().bindTo(!uiBridge.buttonEnabledProp)
        textProperty().bindTo(uiBridge.buttonTextProp)
        setOnAction { presenter.saveButtonClicked() }
    })

}