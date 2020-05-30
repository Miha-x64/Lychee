package net.aquadc.persistence.sql

import net.aquadc.persistence.sql.dialect.sqlite.SqliteDialect
import org.junit.Before


private val db get() = session(SqliteDialect, "jdbc:sqlite::memory:")

class SqlPropSqlite : SqlPropTest() {
    @Before fun init() { session = db }
}
class EmbedRelationsSqlite : EmbedRelationsTest() {
    @Before fun init() { session = db }
}
class QueryBuilderSqlite : QueryBuilderTests() {
    @Before fun init() { session = db }
}
class TemplatesSqlite : TemplatesTest() {
    @Before fun init() { session = db }
}
