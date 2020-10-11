package net.aquadc.persistence.sql

import net.aquadc.persistence.extended.intCollection
import net.aquadc.persistence.extended.uuid
import net.aquadc.persistence.sql.ColMeta.Companion.embed
import net.aquadc.persistence.sql.ColMeta.Companion.nativeType
import net.aquadc.persistence.sql.ColMeta.Companion.type
import net.aquadc.persistence.sql.blocking.Blocking
import net.aquadc.persistence.sql.blocking.Eagerly
import net.aquadc.persistence.sql.blocking.JdbcSession
import net.aquadc.persistence.sql.dialect.postgres.PostgresDialect
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.struct.StructSnapshot
import net.aquadc.persistence.struct.invoke
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.Ilk
import net.aquadc.persistence.type.collection
import net.aquadc.persistence.type.i64
import net.aquadc.persistence.type.serialized
import net.aquadc.persistence.type.string
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.AssumptionViolatedException
import org.junit.Before
import org.junit.Test
import org.postgresql.jdbc.PgConnection
import org.postgresql.util.PGobject
import org.postgresql.util.PSQLException
import java.sql.ResultSet
import java.util.UUID


private val db get() = try {
    // this relies on `host all all 127.0.0.1/32 trust` in pg_hba.conf:
    session(PostgresDialect, "jdbc:postgresql://localhost:5432/test?user=postgres")
} catch (e: PSQLException) {
    throw AssumptionViolatedException("no compatible Postgres database found", e)
}

private inline fun disconnect(session: () -> Session<*>) = try {
    session().close()
} catch (ignored: UninitializedPropertyAccessException) {
    // happens on CI without PostgreSQL
}

class SqlPropPostgres : SqlPropTest() {
    @Before fun init() { session = db }
    @After fun close() { disconnect { session } }
}

class TemplatesPostgres : TemplatesTest() {
    @Before fun init() { session = db }
    @After fun close() { disconnect { session } }

    object Yoozer : Schema<Yoozer>() {
        val Id = "id" let uuid
        val Name = "name" let string
        val Extras = "extras" let SomeSchema
        val Numbers = "numbers" let intCollection
        val MoreNumbers = "more_numbers" let collection(intCollection)
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
        it[Numbers] = intArrayOf(0, 1, 2)
        it[MoreNumbers] = listOf(intArrayOf(1, 2, 3), intArrayOf(4, 5, 6))
    }

    @Test fun <CUR> `just a table`() {
        val Yuzerz = tableOf(Yoozer, "yoozerz1", "_id", i64) { arrayOf(embed(SnakeCase, Extras)) }
        val schema = PostgresDialect.createTable(Yuzerz, true)
        assertEquals(
            """CREATE TEMP TABLE "yoozerz1"
                ("_id" serial8 NOT NULL PRIMARY KEY,
                "id" bytea NOT NULL,
                "name" text NOT NULL,
                "extras_a" text NOT NULL,
                "extras_b" int NOT NULL,
                "extras_c" int8 NOT NULL,
                "numbers" int[] NOT NULL,
                "more_numbers" bytea NOT NULL);""".replace(Regex("\n\\s+"), " "),
            schema
        )
        assertInserts(schema, Yuzerz)
        assertEquals(
            "Some name",
            (session as Session<Blocking<CUR>>)
                .query("SELECT \"name\" FROM \"${Yuzerz.name}\" WHERE \"numbers\" = ?", intCollection, Eagerly.cell<CUR, String>(string))
                .invoke(intArrayOf(0, 1, 2))
        )
    }

    @Test fun <CUR> `custom table`() {
        val Yuzerz = object : Table<Yoozer, Long>(Yoozer, "yoozerz2", "_id", i64) {
            override fun Yoozer.meta(): Array<out ColMeta<Yoozer>> = arrayOf(
                type(pkColumn, "serial NOT NULL"), // this.pkColumn would be inaccessible within lambda
                nativeType(Extras, serialized(SomeSchema))
            )
        }
        val schema = PostgresDialect.createTable(Yuzerz, true)
        assertEquals(
            """CREATE TEMP TABLE "yoozerz2"
                ("_id" serial NOT NULL PRIMARY KEY,
                "id" bytea NOT NULL,
                "name" text NOT NULL,
                "extras" bytea NOT NULL,
                "numbers" int[] NOT NULL,
                "more_numbers" bytea NOT NULL);""".replace(Regex("\n\\s+"), " "),
            schema
        )
        assertInserts(schema, Yuzerz)
        assertEquals(
            "Some name",
            (session as Session<Blocking<CUR>>)
                .query("SELECT \"name\" FROM \"${Yuzerz.name}\" WHERE \"extras\" = ?", serialized(SomeSchema), Eagerly.cell<CUR, String>(string))
                .invoke(sampleYoozer[Yoozer.Extras])
        )
    }

    @Test fun <CUR> `very custom table`() {
        val stmt = (session as JdbcSession).connection.createStatement()
        stmt.execute("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";")
        stmt.close()

        val someJsonb: Ilk<Struct<SomeSchema>, SomeSchema> = nativeType(
            "jsonb NOT NULL", SomeSchema,
            { p1 -> PGobject("jsonb", """["${p1[SomeSchema.A]}", ${p1[SomeSchema.B]}, ${p1[SomeSchema.C]}]""") },
            { p ->
                (p as PGobject).value.trim('[', ']').split(", ").let { tokens ->
                    SomeSchema {
                        it[A] = tokens[0].trim('"')
                        it[B] = tokens[1].toInt()
                        it[C] = tokens[2].toLong()
                    }
                }
            }
        )
        val intMatrix: Ilk<
            List<IntArray>,
            DataType.NotNull.Collect<
                List<IntArray>,
                IntArray,
                DataType.NotNull.Collect<IntArray, Int, DataType.NotNull.Simple<Int>>
                >
            >
            = nativeType(
            "int[][] NOT NULL", collection(intCollection),
            { p1 -> (session as JdbcSession).connection.unwrap(PgConnection::class.java).createArrayOf("int", p1.toTypedArray()) },
            { p -> ((p as java.sql.Array).array as Array<*>).map { (it as Array<Int>).toIntArray() } }
            // never cast to Array<Array<Int>>: ^^^^^^^^^^^ empty array will be returned as Array<Int>
        )
        val Yoozerz = tableOf(Yoozer, "yoozerz3", Yoozer.Id) { arrayOf(
            nativeType(Id, "uuid NOT NULL DEFAULT uuid_generate_v4()"),
            type(Name, "varchar(128) NOT NULL"),
            nativeType(Extras, someJsonb),
            nativeType(MoreNumbers, intMatrix)
        ) }

        val schema = PostgresDialect.createTable(Yoozerz, true)
        assertEquals(
            """CREATE TEMP TABLE "yoozerz3"
                ("id" uuid NOT NULL DEFAULT uuid_generate_v4() PRIMARY KEY,
                "name" varchar(128) NOT NULL,
                "extras" jsonb NOT NULL,
                "numbers" int[] NOT NULL,
                "more_numbers" int[][] NOT NULL);""".replace(Regex("\n\\s+"), " "),
            schema
        )
        assertInserts(schema, Yoozerz)
        assertEquals(
            "Some name",
            (session as Session<Blocking<CUR>>)
                .query("SELECT \"name\" FROM \"${Yoozerz.name}\" WHERE \"id\" = ? AND \"extras\" = ?",
                    nativeType("uuid", uuid),
                    someJsonb,
                    Eagerly.cell<CUR, String>(string))
                .invoke(sampleYoozer[Yoozer.Id], sampleYoozer[Yoozer.Extras])
        )
    }
    private fun <ID : IdBound> assertInserts(create: String, table: Table<Yoozer, ID>) {
        session.withTransaction {
            (session as JdbcSession).connection.createStatement().run {
                execute(create)
                close()
            }
            val pk = insert(table, sampleYoozer)
            val rec = (session as Session<Blocking<ResultSet>>).query<Blocking<ResultSet>, ID, StructSnapshot<Yoozer>>(
                "SELECT ${table.managedColNames.joinToString()} FROM ${table.name} WHERE ${table.idColName} = ?",
                table.idColType,
                Eagerly.struct<ResultSet, Yoozer>(table, BindBy.Name)
            )(pk)
            assertNotSame(sampleYoozer, rec)
            assertEquals(sampleYoozer, rec)
        }
    }
}
