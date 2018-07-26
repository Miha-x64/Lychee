package net.aquadc.properties.fx.sqlSample

import net.aquadc.properties.sql.*
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement


fun main(args: Array<String>) {
    DriverManager.getConnection("jdbc:sqlite:sample.db").use { conn ->
        createNeededTables(conn)
        val sess = JdbcSqliteSession(conn)
        fillIfEmpty(sess)
    }
}

private fun createNeededTables(conn: Connection) {
    Tables.forEach { table ->
        conn.createStatement().use { statement ->
            statement.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='${table.name}'").use {
                if (it.next()) {
                    println("the table ${table.name} already exists")
                } else {
                    create(statement, table)
                    println("table ${table.name} was created")
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
    // todo check whether empty
    session.transaction { transaction ->
        transaction.insertHuman("Stephen", "Hawking", null)
    }
}
