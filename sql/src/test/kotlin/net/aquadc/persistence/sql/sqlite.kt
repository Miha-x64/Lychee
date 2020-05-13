package net.aquadc.persistence.sql

import net.aquadc.persistence.sql.blocking.Blocking
import net.aquadc.persistence.sql.dialect.sqlite.SqliteDialect


private val db = session(SqliteDialect, "jdbc:sqlite::memory:")

class SqlPropSqlite : SqlPropTest() {
    override val session: Session<*>
        get() = db
}
class EmbedRelationsSqlite : EmbedRelationsTest() {
    override val session: Session<*>
        get() = db
}
class QueryBuilderSqlite : QueryBuilderTests() {
    override val session: Session<*>
        get() = db
}
class TemplatesSqlite : TemplatesTest() {
    override val session: Session<out Blocking<*>>
        get() = db
}
