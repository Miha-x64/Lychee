package net.aquadc.persistence.sql

import net.aquadc.persistence.extended.either.EitherLeft
import net.aquadc.persistence.extended.either.EitherRight
import net.aquadc.persistence.extended.either.fold
import net.aquadc.persistence.sql.blocking.Blocking
import net.aquadc.persistence.sql.dialect.postgres.PostgresDialect
import org.junit.AssumptionViolatedException
import org.postgresql.util.PSQLException


private val _db = try {
    // this relies on `host all all 127.0.0.1/32 trust` in pg_hba.conf:
    EitherRight(session(PostgresDialect, "jdbc:postgresql://localhost:5432/test?user=postgres"))
} catch (e: PSQLException) {
    EitherLeft(e)
}
private val db get() = _db.fold(
    { throw AssumptionViolatedException("no compatible Postgres database found", it) },
    { it }
)

class SqlPropPostgres : SqlPropTest() {
    override val session: Session<*> get() = db
//    override val duplicatePkExceptionClass: Class<*> get() =
}

class EmbedRelationsPostgres : EmbedRelationsTest() {
    override val session: Session<*> get() = db
}
class QueryBuilderPostgres : QueryBuilderTests() {
    override val session: Session<*> get() = db
}
class TemplatesPostgres : TemplatesTest() {
    override val session: Session<out Blocking<*>> get() = db
}
