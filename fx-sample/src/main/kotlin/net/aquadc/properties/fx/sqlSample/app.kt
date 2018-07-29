package net.aquadc.properties.fx.sqlSample

import com.jfoenix.controls.JFXListCell
import com.jfoenix.controls.JFXListView
import javafx.application.Application
import javafx.collections.FXCollections
import javafx.scene.Scene
import javafx.stage.Stage
import net.aquadc.properties.fx.fx
import net.aquadc.properties.mapWith
import net.aquadc.properties.sql.*
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement


class SqliteApp : Application() {

    private val connection = DriverManager.getConnection("jdbc:sqlite:sample.db").also(::createNeededTables)
    private val sess = JdbcSqliteSession(connection).also(::fillIfEmpty)

    override fun start(stage: Stage) {
        stage.scene = Scene(
                JFXListView<Human>().apply {
                    items = FXCollections.observableArrayList(sess.select(HumanTable).value)
                    setCellFactory { object : JFXListCell<Human>() {
                        override fun updateItem(item: Human?, empty: Boolean) {
                            super.updateItem(item, empty)
                            if (item != null) {
                                textProperty().bind(item.name.mapWith(item.surname) { n, s -> "$n $s" }.fx())
                            }
                        }
                    } }
                },
                400.0, 600.0)
        stage.show()
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
        sb.append(col.name).append(' ').append(Java2SQLite[col.javaType]!!)
        if (col === idCol) sb.append(" PRIMARY KEY")
        else if (!col.isNullable) sb.append(" NOT NULL")
        sb.append(", ")
    }
    sb.setLength(sb.length - 2) // trim last comma
    sb.append(");")

    statement.execute(sb.toString())
}

private fun fillIfEmpty(session: Session) {
    if (session.count(HumanTable).value == 0L) {
        session.transaction { transaction ->
            transaction.insertHuman("Stephen", "Hawking", null)
            transaction.insertHuman("Albert", "Einstein", null)
            transaction.insertHuman("Dmitri", "Mendeleev", null)
        }
    }
}
