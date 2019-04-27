package net.aquadc.properties.sql

import net.aquadc.persistence.struct.build
import net.aquadc.persistence.type.long
import net.aquadc.properties.sql.dialect.sqlite.SqliteDialect
import java.sql.DriverManager


val SomeTable = SimpleTable(SomeSchema, "some_table", "_id", long)

val session = JdbcSession(DriverManager.getConnection("jdbc:sqlite::memory:").also { conn ->
    val stmt = conn.createStatement()
    stmt.execute(SqliteDialect.createTable(SomeTable))
    stmt.close()
}, SqliteDialect)

val SomeDao = session[SomeTable]

fun createTestRecord() =
        session.withTransaction {
            insert(SomeTable, SomeSchema.build {
                it[A] = "first"
                it[B] = 2
                it[C] = 3
            })
        }
