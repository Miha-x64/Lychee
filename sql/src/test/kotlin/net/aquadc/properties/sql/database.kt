package net.aquadc.properties.sql

import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.build
import net.aquadc.persistence.type.int
import net.aquadc.persistence.type.long
import net.aquadc.persistence.type.string
import net.aquadc.properties.sql.dialect.sqlite.SqliteDialect
import java.sql.DriverManager


object SomeSchema : Schema<SomeSchema>() {
    val A = "a" let string
    val B = "b" let int
    val C = "c" mut long
}
val SomeTable = SimpleTable(SomeSchema, "some_table", "_id", long)

object SchWithId : Schema<SchWithId>() {
    val Id = "_id" let long
    val Value = "value" let string
    val MutValue = "mutValue".mut(string, default = "")
}
val TableWithId = SimpleTable(SchWithId, "with_id", SchWithId.Id)

object WithNested : Schema<WithNested>() {
    val OwnField = "q" let string
    val Embedded = "w" mut SchWithId
    val OtherOwnField = "e" let long
}
val TableWithEmbed = object : SimpleTable<WithNested, Long>(WithNested, "with_nested", "_id", long) {
    override fun relations(): List<Relation<WithNested, Long, *>> = listOf(
            Relation.Embedded(SnakeCase, WithNested.Embedded)
    )
}

val session = JdbcSession(DriverManager.getConnection("jdbc:sqlite::memory:").also { conn ->
    val stmt = conn.createStatement()
    arrayOf(SomeTable, TableWithId, TableWithEmbed).forEach {
        stmt.execute(SqliteDialect.createTable(it))
    }
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
