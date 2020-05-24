package net.aquadc.persistence.sql

import net.aquadc.persistence.extended.either.EitherLeft
import net.aquadc.persistence.extended.either.EitherRight
import net.aquadc.persistence.extended.either.fold
import net.aquadc.persistence.extended.uuid
import net.aquadc.persistence.sql.ColMeta.Companion.embed
import net.aquadc.persistence.sql.ColMeta.Companion.nativeType
import net.aquadc.persistence.sql.ColMeta.Companion.type
import net.aquadc.persistence.sql.blocking.Blocking
import net.aquadc.persistence.sql.dialect.postgres.PostgresDialect
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.struct.invoke
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.collection
import net.aquadc.persistence.type.i32
import net.aquadc.persistence.type.i64
import net.aquadc.persistence.type.serialized
import net.aquadc.persistence.type.string
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.AssumptionViolatedException
import org.junit.Test
import org.postgresql.jdbc.PgConnection
import org.postgresql.util.PGobject
import org.postgresql.util.PSQLException
import java.util.UUID


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

    object Yoozer : Schema<Yoozer>() {
        val Id = "id" let uuid
        val Name = "name" let string
        val Extras = "extras" let SomeSchema
        val Numbers = "numbers" let collection(i32)
    }
    private fun PGobject(type: String, value: String) = PGobject().also {
        it.type = type
        it.value = value
    }
    private val sampleYoozer = Yoozer {
        it[Id] = UUID.randomUUID()
        it[Name] = "Some name"
        it[Extras] = SomeSchema {
            it[A] = "qwe"
            it[B] = 123
            it[C] = 10_987_654_321L
        }
        it[Numbers] = intArrayOf(1, 2, 3).asList()
    }

    @Test fun `just a table`() {
        val Yuzerz = tableOf(Yoozer, "yoozerz1", "_id", i64) { arrayOf(embed(SnakeCase, Extras)) }
        val schema = PostgresDialect.createTable(Yuzerz)
        assertEquals(
            """CREATE TABLE "yoozerz1"
                ("_id" serial8 NOT NULL PRIMARY KEY,
                "id" bytea NOT NULL,
                "name" text NOT NULL,
                "extras_a" text NOT NULL,
                "extras_b" int NOT NULL,
                "extras_c" int8 NOT NULL,
                "numbers" bytea NOT NULL);""".replace(Regex("\n\\s+"), " "),
            schema
        )
        assertInserts(schema, Yuzerz)
    }

    @Test fun `custom table`() {
        val Yuzerz = object : Table<Yoozer, Long>(Yoozer, "yoozerz2", "_id", i64) {
            override fun Yoozer.meta(): Array<out ColMeta<Yoozer>> = arrayOf(
                type(pkColumn, "serial NOT NULL"), // this.pkColumn would be inaccessible within lambda
                nativeType(Extras, serialized(SomeSchema))
            )
        }
        val schema = PostgresDialect.createTable(Yuzerz)
        assertEquals(
            """CREATE TABLE "yoozerz2"
                ("_id" serial NOT NULL PRIMARY KEY,
                "id" bytea NOT NULL,
                "name" text NOT NULL,
                "extras" bytea NOT NULL,
                "numbers" bytea NOT NULL);""".replace(Regex("\n\\s+"), " "),
            schema
        )
        assertInserts(schema, Yuzerz)
    }

    @Test fun `very custom table`() {
        val stmt = db.connection.createStatement()
        stmt.execute("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";")
        stmt.close()

        val someJsonb = object : NativeType<Struct<SomeSchema>, SomeSchema>("jsonb NOT NULL", SomeSchema) {
            override fun invoke(p1: Struct<SomeSchema>): Any? =
                PGobject("jsonb", """["${p1[SomeSchema.A]}", ${p1[SomeSchema.B]}, ${p1[SomeSchema.C]}]""")
            override fun back(p: Any?): Struct<SomeSchema> =
                (p as PGobject).value.trim('[', ']').split(", ").let { tokens ->
                    SomeSchema {
                        it[A] = tokens[0].trim('"')
                        it[B] = tokens[1].toInt()
                        it[C] = tokens[2].toLong()
                    }
                }
        }
        val intArray = object : NativeType<List<Int>, DataType.NotNull.Collect<List<Int>, Int, DataType.NotNull.Simple<Int>>>("int ARRAY NOT NULL", collection(i32)) {
            override fun invoke(p1: List<Int>): Any? =
                db.connection.unwrap(PgConnection::class.java).createArrayOf("int", p1.toIntArray())
            override fun back(p: Any?): List<Int> =
                ((p as java.sql.Array).array as Array<Int>).asList()
        }
        val Yoozerz = tableOf(Yoozer, "yoozerz3", Yoozer.Id) { arrayOf(
            nativeType(Id, "uuid NOT NULL DEFAULT uuid_generate_v4()"),
            type(Name, "varchar(128) NOT NULL"),
            nativeType(Extras, someJsonb),
            nativeType(Numbers, intArray)
        ) }

        val schema = PostgresDialect.createTable(Yoozerz)
        assertEquals(
            """CREATE TABLE "yoozerz3"
                ("id" uuid NOT NULL DEFAULT uuid_generate_v4() PRIMARY KEY,
                "name" varchar(128) NOT NULL,
                "extras" jsonb NOT NULL,
                "numbers" int ARRAY NOT NULL);""".replace(Regex("\n\\s+"), " "),
            schema
        )
        assertInserts(schema, Yoozerz)
    }
    private fun assertInserts(create: String, table: Table<Yoozer, *>) {
        db.withTransaction {
            try {
                db.connection.createStatement().run {
                    execute(create)
                    close()
                }

                val rec = insert(table, sampleYoozer)
                assertNotSame(sampleYoozer, rec)
                assertEquals(sampleYoozer, rec)
            } finally {
                db.connection.createStatement().run {
                    execute("DROP TABLE " + table.name)
                    close()
                }
            }
        }
    }
}
