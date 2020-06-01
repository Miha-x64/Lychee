package net.aquadc.properties.fx.sqlSample

import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXListCell
import com.jfoenix.controls.JFXListView
import com.jfoenix.controls.JFXTextField
import javafx.application.Application
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.stage.Stage
import net.aquadc.persistence.sql.Record
import net.aquadc.persistence.sql.Table
import net.aquadc.persistence.sql.blocking.JdbcSession
import net.aquadc.persistence.sql.dialect.Dialect
import net.aquadc.persistence.sql.dialect.sqlite.SqliteDialect
import net.aquadc.properties.fx.bindEnableTo
import net.aquadc.properties.fx.bindTextTo
import net.aquadc.properties.fx.bindTo
import net.aquadc.properties.fx.fx
import net.aquadc.properties.fx.fxList
import net.aquadc.properties.fx.setWhenClicked
import net.aquadc.propertiesSampleLogic.sql.Human
import net.aquadc.propertiesSampleLogic.sql.SampleTables
import net.aquadc.propertiesSampleLogic.sql.SqlViewModel
import java.sql.Connection
import java.sql.DriverManager


fun startSqlSample(stage: Stage) {
    val dialect = SqliteDialect
    val connection = DriverManager.getConnection("jdbc:sqlite:sample.db").also { createNeededTables(it, dialect) }
    val sess = JdbcSession(connection, dialect)
    val vm = SqlViewModel(sess)
    stage.titleProperty().bind(vm.titleProp.fx())
    stage.scene = Scene(
        HBox().apply {

            val hBox = this

            children += JFXListView<Record<Human, Long>>().apply {
                items = vm.humanListProp.fxList()
                setCellFactory(vm::createListCell)
                prefWidthProperty().bind(hBox.widthProperty().multiply(.4))
                vm.lastInserted.addChangeListener { _, inserted -> selectionModel.select(inserted) }
                vm.selectedProp.bindTo(selectionModel.selectedItemProperty())
            }

            children += VBox().apply {
                prefWidthProperty().bind(hBox.widthProperty().multiply(.6))

                padding = Insets(10.0, 10.0, 10.0, 10.0)

                children += JFXTextField().apply {
                    bindEnableTo(vm.actionsEnabledProp)
                    vm.nameProp.addChangeListener { _, new -> if (text != new) text = new }
                    textProperty().addListener { _, _, newText -> vm.editableNameProp.value = newText }
                }

                children += Label().apply {
                    padding = Insets(10.0, 0.0, 0.0, 0.0)
                    bindTextTo(vm.airConditionersTextProp)
                }

                children += JFXButton("Delete").apply {
                    bindEnableTo(vm.actionsEnabledProp)
                    setWhenClicked(vm.deleteClicked)
                }

                children += Pane().apply {
                    isFillHeight = true
                    VBox.setVgrow(this, Priority.ALWAYS)
                }

                children += HBox().apply {
                    children += JFXButton("Create new").apply {
                        setWhenClicked(vm.createClicked)
                    }

                    children += JFXButton("Dump debug info").apply {
                        setOnMouseClicked { _ ->
                            println(buildString(sess::dump))
                        }
                    }

                    children += JFXButton("Truncate").apply {
                        setWhenClicked(vm.truncateClicked)
                    }
                }
            }

        },
        500.0, 400.0)
    stage.setOnCloseRequest { connection.close() }
    stage.show()
}

private fun SqlViewModel.createListCell(lv: ListView<Record<Human, Long>>): JFXListCell<Record<Human, Long>> {
    val cell = object : JFXListCell<Record<Human, Long>>() {
        override fun updateItem(item: Record<Human, Long>?, empty: Boolean) {
            textProperty().unbind()
            super.updateItem(item, empty)
            if (item != null && !empty) {
                graphic = null
                textProperty().bind(nameSurnameProp(item).fx())
            }
        }
    }
    cell.setOnMouseClicked { ev -> if (cell.isEmpty) ev.consume() }
    return cell
}


private fun createNeededTables(conn: Connection, dialect: Dialect) {
    SampleTables.forEach { table: Table<*, Long> ->
        conn.createStatement().use { statement ->
            statement.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='${table.name}'").use {
                if (it.next()) {
                    println("the table `${table.name}` already exists")
                } else {
                    statement.execute(dialect.createTable(table))
                    println("table `${table.name}` was created")
                }
            }
        }
    }
}
