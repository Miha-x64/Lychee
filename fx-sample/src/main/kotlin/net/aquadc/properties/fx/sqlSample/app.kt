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
import javafx.scene.layout.*
import javafx.stage.Stage
import net.aquadc.persistence.sql.JdbcSession
import net.aquadc.persistence.sql.Table
import net.aquadc.properties.*
import net.aquadc.properties.fx.bindTo
import net.aquadc.properties.fx.fx
import net.aquadc.properties.fx.fxList
import net.aquadc.properties.fx.setWhenClicked
import net.aquadc.persistence.sql.dialect.Dialect
import net.aquadc.persistence.sql.dialect.sqlite.SqliteDialect
import net.aquadc.propertiesSampleLogic.sql.*
import java.sql.Connection
import java.sql.DriverManager


class SqliteApp : Application() {

    private val dialect = SqliteDialect
    private val connection = DriverManager.getConnection("jdbc:sqlite:sample.db").also { createNeededTables(it, dialect) }
    private val sess = JdbcSession(connection, dialect)
    private val vm = SqlViewModel(sess)

    override fun start(stage: Stage) {
        stage.titleProperty().bind(vm.titleProp.fx())
        stage.scene = Scene(
                HBox().apply {

                    val hBox = this

                    children += JFXListView<Human>().apply {
                        items = vm.humanListProp.fxList()
                        setCellFactory(::createListCell)
                        prefWidthProperty().bind(hBox.widthProperty().multiply(.4))
                        vm.lastInserted.addChangeListener { _, inserted -> selectionModel.select(inserted) }
                        vm.selectedProp.bindTo(selectionModel.selectedItemProperty())
                    }

                    children += VBox().apply {
                        prefWidthProperty().bind(hBox.widthProperty().multiply(.6))

                        padding = Insets(10.0, 10.0, 10.0, 10.0)

                        children += JFXTextField().apply {
                            disableProperty().bind((!vm.actionsEnabledProp).fx())
                            vm.nameProp.addChangeListener { _, new -> if (text != new) text = new }
                            textProperty().addListener { _, _, newText -> vm.editableNameProp.value = newText }
                        }

                        children += Label().apply {
                            padding = Insets(10.0, 0.0, 0.0, 0.0)
                            textProperty().bind(vm.airConditionersTextProp.fx())
                        }

                        children += JFXButton("Delete").apply {
                            disableProperty().bind((!vm.actionsEnabledProp).fx())
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
        stage.show()
    }

    private fun createListCell(lv: ListView<Human>): JFXListCell<Human> {
        val cell = object : JFXListCell<Human>() {
            override fun updateItem(item: Human?, empty: Boolean) {
                textProperty().unbind()
                super.updateItem(item, empty)
                if (item != null && !empty) {
                    graphic = null
                    textProperty().bind(vm.nameSurnameProp(item).fx())
                }
            }
        }
        cell.setOnMouseClicked { ev -> if (cell.isEmpty) ev.consume() }
        return cell
    }

    override fun stop() {
        connection.close()
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) {
            launch(SqliteApp::class.java)
        }
    }

}


private fun createNeededTables(conn: Connection, dialect: Dialect) {
    SampleTables.forEach { table: Table<*, Long, *> ->
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
