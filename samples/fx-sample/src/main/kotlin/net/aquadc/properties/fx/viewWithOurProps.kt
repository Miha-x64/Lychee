package net.aquadc.properties.fx

import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXTextField
import javafx.geometry.Insets
import javafx.scene.control.Label
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.stage.Stage
import net.aquadc.properties.fx.sqlSample.startSqlSample
import net.aquadc.properties.map
import net.aquadc.properties.not
import net.aquadc.propertiesSampleLogic.MainVm


fun viewWithOurProps(vm: MainVm) = VBox(10.0).apply {

    padding = Insets(10.0, 10.0, 10.0, 10.0)

    children.add(JFXTextField().apply {
        promptText = "Email"
        textProperty().bindBidirectional(vm.emailProp.fx())
    })

    children.add(Label().apply {
        text = "E-mail is invalid"
        bindVisibilityHardlyTo(!vm.emailValidProp)
    })

    children.add(JFXTextField().apply {
        promptText = "Name"
        textProperty().bindBidirectional(vm.nameProp.fx())
    })

    children.add(JFXTextField().apply {
        promptText = "Surname"
        textProperty().bindBidirectional(vm.surnameProp.fx())
    })

    children.add(JFXButton().apply {
        disableProperty().bind((!vm.buttonEnabledProp).fx())
        textProperty().bind(vm.buttonEnabledProp.map {
            if (it) "Save changes" else "Nothing changed"
        }.fx())
        setWhenClicked(vm.buttonClickedProp)
    })

    children.add(Pane().also {
        VBox.setVgrow(it, Priority.ALWAYS)
    })

    children.add(JFXButton("Open user list").apply {
        setOnAction { startSqlSample(Stage()) }
    })

}
