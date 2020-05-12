package net.aquadc.persistence.sql

import net.aquadc.persistence.extended.Tuple
import net.aquadc.persistence.extended.Tuple3
import net.aquadc.persistence.extended.either.plus
import net.aquadc.persistence.extended.partial
import net.aquadc.persistence.sql.ColMeta.Companion.embed
import net.aquadc.persistence.sql.blocking.JdbcSession
import net.aquadc.persistence.sql.dialect.sqlite.SqliteDialect
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.invoke
import net.aquadc.persistence.type.i32
import net.aquadc.persistence.type.i64
import net.aquadc.persistence.type.nullable
import net.aquadc.persistence.type.string
import java.sql.DriverManager


object SomeSchema : Schema<SomeSchema>() {
    val A = "a" let string
    val B = "b" let i32
    val C = "c" mut i64
}
val SomeTable = tableOf(SomeSchema, "some_table", "_id", i32)

object SchWithId : Schema<SchWithId>() {
    val Id = "_id" let i32
    val Value = "value" let string
    val MutValue = "mutValue".mut(string, default = "")
}
val TableWithId = tableOf(SchWithId, "with_id", SchWithId.Id)

object WithNested : Schema<WithNested>() {
    val OwnField = "q" let string
    val Nested = "w" mut SchWithId
    val OtherOwnField = "e" let i64
}
val TableWithEmbed = tableOf(WithNested, "with_nested", "_id", i64) {
    arrayOf(
        embed(SnakeCase, Nested)
    )
}

object DeeplyNested : Schema<DeeplyNested>() {
    val OwnField = "own" let string
    val Nested = "nest" mut WithNested
}
val TableWithDeepEmbed = tableOf(DeeplyNested, "deep", "_id", i64) {
    arrayOf(
        embed(SnakeCase, Nested),
        embed(SnakeCase, Nested / WithNested.Nested)
    )
}

val goDeeper = Tuple3(
        "first", DeeplyNested,
        "second", WithNullableNested,
        "third", string + SomeSchema
)

val WeNeedToGoDeeper = tableOf(goDeeper, "deeper", "_id", i64) {
    arrayOf(
        embed(SnakeCase, First)
      , embed(SnakeCase, First / DeeplyNested.Nested)
      , embed(SnakeCase, First / DeeplyNested.Nested / WithNested.Nested)
      , embed(SnakeCase, Second)
      , embed(SnakeCase, Second / WithNullableNested.Nested, "fieldSet")
      , embed(SnakeCase, Third, "which")
      , embed(SnakeCase, Third % goDeeper.typeOf(goDeeper.Third).schema.Second, "fieldSet")
    )
}


object WithNullableNested : Schema<WithNullableNested>() {
    val OwnField = "q" let string
    val Nested = "w" mut nullable(SchWithId)
    val OtherOwnField = "e" let i64
}
val TableWithNullableEmbed = tableOf(WithNullableNested, "with_nullable_nested", "_id", i64) {
    arrayOf(
        embed(SnakeCase, Nested, "nested_fields")
    )
}

object WithPartialNested : Schema<WithPartialNested>() {
    val Nested = "nested" mut partial(SchWithId)
    val OwnField = "q" let string
}
val TableWithPartialEmbed = tableOf(WithPartialNested, "with_partial_nested", "_id", i64) {
    arrayOf(
        embed(SnakeCase, Nested, "nested_fields")
    )
}

object WithEverything : Schema<WithEverything>() {
    val Nest1 = "nest1" let nullable(partial(WithPartialNested))
    val Nest2 = "nest2" mut SchWithId
}
val TableWithEverything = tableOf(WithEverything, "with_everything", "_id", i64) {
    arrayOf(
        embed(SnakeCase, Nest1, "fields"),
        embed(SnakeCase, Nest1 / WithPartialNested.Nested, "fields"),
        embed(SnakeCase, Nest2)
    )
}

val stringOrNullableString = string + nullable(string)
val schemaWithNullableEither = Tuple("1", stringOrNullableString, "2", nullable(stringOrNullableString))
val TableWithNullableEither = tableOf(schemaWithNullableEither, "tableContainingEither","_id", i64) {
    arrayOf(
        embed(SnakeCase, First, "which"),
        embed(SnakeCase, Second, "whetherAndWhich")
    )
}

// for Templates test
val User = Tuple("name", string, "email", string)
val UserTable = tableOf(User, "users", "_id", i64)

val Contact = Tuple("value", string, "user_id", i64)
val ContactTable = tableOf(Contact, "contacts", "_id", i64)


val TestTables = arrayOf(
        SomeTable, TableWithId, TableWithEmbed, TableWithDeepEmbed, WeNeedToGoDeeper,
        TableWithNullableEmbed, TableWithPartialEmbed, TableWithEverything, TableWithNullableEither,
        UserTable, ContactTable
)

val jdbcSession by lazy { // init only when requested, unused in Robolectric tests
    JdbcSession(DriverManager.getConnection("jdbc:sqlite::memory:").also { conn ->
        val stmt = conn.createStatement()
        TestTables.forEach {
            stmt.execute(SqliteDialect.createTable(it))
        }
        stmt.close()
    }, SqliteDialect)
}

fun Session<*>.createTestRecord() =
        withTransaction {
            insert(SomeTable, SomeSchema {
                it[A] = "first"
                it[B] = 2
                it[C] = 3
            })
        }
