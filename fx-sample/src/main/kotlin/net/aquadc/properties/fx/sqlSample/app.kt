package net.aquadc.properties.fx.sqlSample

import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXListCell
import com.jfoenix.controls.JFXListView
import com.jfoenix.controls.JFXTextField
import javafx.application.Application
import javafx.beans.binding.Bindings
import javafx.beans.property.ReadOnlyObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.layout.*
import javafx.stage.Stage
import net.aquadc.properties.*
import net.aquadc.properties.fx.fx
import net.aquadc.properties.sql.*
import net.aquadc.properties.sql.dialect.sqlite.SqliteDialect
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement
import java.util.concurrent.Callable


class SqliteApp : Application() {

    private val connection = DriverManager.getConnection("jdbc:sqlite:sample.db").also(::createNeededTables)
    private val sess = JdbcSession(connection, SqliteDialect).also(::fillIfEmpty)

    override fun start(stage: Stage) {
        stage.titleProperty().bind(
                sess[HumanTable].count().fx().map { "Sample SQLite application ($it records)" }
        )
        stage.scene = Scene(
                HBox().apply {

                    val hBox = this

                    val humanListProp = sess[HumanTable].select()
                    val humanList = FXCollections.observableArrayList(humanListProp.value)
                    humanListProp.addChangeListener { _, new ->
                        humanList.clear()
                        humanList.addAll(new)
                    }
                    val listView = JFXListView<Human>().apply {
                        items = humanList
                        setCellFactory(::createListCell)
                        prefWidthProperty().bind(hBox.widthProperty().multiply(.4))
                    }
                    children += listView

                    children += VBox().apply {
                        prefWidthProperty().bind(hBox.widthProperty().multiply(.6))

                        padding = Insets(10.0, 10.0, 10.0, 10.0)

                        val selProp: ReadOnlyObjectProperty<Human?> = listView.selectionModel.selectedItemProperty()
                        val namePatch = propertyOf(mapOf<Human, String>())
                        namePatch.debounced(1000L).onEach { new ->
                            if (new.isNotEmpty() && namePatch.casValue(new, mapOf())) {
                                sess.withTransaction {
                                    new.forEach { (human, newName) ->
                                        if (human.isManaged) { // if it was just deleted, ignore
                                            human.nameProp.set(newName)
                                        }
                                    }
                                }
                            }
                        }

                        val actionsDisabledProp = selProp.isNull

                        children += JFXTextField().apply {
                            val nameProp = SimpleStringProperty()
                            selProp.addListener { _, _, it ->
                                nameProp.unbind()
                                if (it == null) nameProp.set("")
                                else nameProp.bind(it.nameProp.fx())
                            }
                            disableProperty().bind(actionsDisabledProp)
                            selProp.addListener { _, _, human ->
                                text = human?.nameProp?.value ?: ""
                            }
                            textProperty().addListener { _, _, newText ->
                                selProp.value?.let {
                                    namePatch += it to newText
                                }
                            }
                        }

                        children += Label().apply {
                            padding = Insets(10.0, 0.0, 0.0, 0.0)
                            val conditionersProp = SimpleStringProperty()
                            selProp.addListener { _, _, sel ->
                                conditionersProp.unbind()
                                if (sel == null) {
                                    conditionersProp.set("none")
                                } else {
                                    conditionersProp.bind(sel.carsProp.map {
                                        "Air conditioner(s) in car(s): [\n" +
                                                it.map { it.conditionerModelProp.value + '\n' }.joinToString() + ']'
                                    }.fx())
                                }
                            }
                            textProperty().bind(conditionersProp)
                        }

                        children += JFXButton("Delete").apply {
                            disableProperty().bind(actionsDisabledProp)

                            setOnMouseClicked { _ ->
                                sess.withTransaction {
                                    delete(selProp.value!!)
                                }
                            }
                        }

                        children += Pane().apply {
                            isFillHeight = true
                            VBox.setVgrow(this, Priority.ALWAYS)
                        }

                        children += HBox().apply {
                            children += JFXButton("Create new").apply {
                                setOnMouseClicked { _ ->
                                    listView.selectionModel.select(
                                            sess.withTransaction { insertHuman("", "") }
                                    )
                                }
                            }

                            children += JFXButton("Dump debug info").apply {
                                setOnMouseClicked { _ ->
                                    println(StringBuilder().also(sess::dump).toString())
                                }
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
                    textProperty().bind(item.nameProp.mapWith(item.surnameProp) { n, s -> "$n $s" }.fx())
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


private inline fun <T, R> ObservableValue<T>.map(crossinline transform: (T) -> R) =
        Bindings.createObjectBinding(Callable<R> { transform(value) }, this)


private fun createNeededTables(conn: Connection) {
    Tables.forEach { table ->
        conn.createStatement().use { statement ->
            statement.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='${table.name}'").use {
                if (it.next()) {
                    println("the table `${table.name}` already exists")
                } else {
                    create(statement, table)
                    println("table `${table.name}` was created")
                }
            }
        }
    }
}

private fun create(statement: Statement, table: Table<*, *>) {
    val sb = StringBuilder("CREATE TABLE ").append(table.name).append(" (")
    val idCol = table.idCol
    check(table.columns.isNotEmpty())
    table.columns.forEach { col ->
        sb.append(col.name).append(' ').append(col.converter.javaType)
        if (col === idCol) sb.append(" PRIMARY KEY")
        else if (!col.converter.isNullable) sb.append(" NOT NULL")
        sb.append(", ")
    }
    sb.setLength(sb.length - 2) // trim last comma
    sb.append(");")

    statement.execute(sb.toString())
}

private fun fillIfEmpty(session: Session) {
    if (session[HumanTable].count().value == 0L) {
        session.withTransaction {
            insertHuman("Stephen", "Hawking")
            val relativist = insertHuman("Albert", "Einstein")
            insertHuman("Dmitri", "Mendeleev")
            val electrician = insertHuman("Nikola", "Tesla")

            // don't know anything about their friendship, just a sample
            insert(FriendTable,
                    FriendTable.LeftId - relativist.primaryKey, FriendTable.RightId - electrician.primaryKey
            )

            val car = insertCar(electrician)
            car.conditionerModelProp.set("the coolest air cooler")
        }
    }
}
