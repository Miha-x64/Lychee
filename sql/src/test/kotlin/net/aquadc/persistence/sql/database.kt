package net.aquadc.persistence.sql

import net.aquadc.persistence.extended.Tuple3
import net.aquadc.persistence.extended.either.either
import net.aquadc.persistence.extended.partial
import net.aquadc.persistence.sql.dialect.sqlite.SqliteDialect
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.build
import net.aquadc.persistence.type.int
import net.aquadc.persistence.type.long
import net.aquadc.persistence.type.nullable
import net.aquadc.persistence.type.string
import java.sql.DriverManager


object SomeSchema : Schema<SomeSchema>() {
    val A = "a" let string
    val B = "b" let int
    val C = "c" mut long
}
val SomeTable = tableOf(SomeSchema, "some_table", "_id", long)

object SchWithId : Schema<SchWithId>() {
    val Id = "_id" let long
    val Value = "value" let string
    val MutValue = "mutValue".mut(string, default = "")
}
val TableWithId = tableOf(SchWithId, "with_id", SchWithId.Id)

object WithNested : Schema<WithNested>() {
    val OwnField = "q" let string
    val Nested = "w" mut SchWithId
    val OtherOwnField = "e" let long
}
val TableWithEmbed = tableOf(WithNested, "with_nested", "_id", long) {
    arrayOf(
            Relation.Embedded(SnakeCase, WithNested.Nested)
    )
}

object DeeplyNested : Schema<DeeplyNested>() {
    val OwnField = "own" let string
    val Nested = "nest" mut WithNested
}
val TableWithDeepEmbed = tableOf(DeeplyNested, "deep", "_id", long) {
    arrayOf(
            Relation.Embedded(SnakeCase, DeeplyNested.Nested),
            Relation.Embedded(SnakeCase, DeeplyNested.Nested / WithNested.Nested)
    )
}

val goDeeper = Tuple3(
        "first", DeeplyNested,
        "second", WithNullableNested,
        "third", either(
                "left", string,
                "right", SomeSchema
        )
)

val WeNeedToGoDeeper = tableOf(goDeeper, "deeper", "_id", long) {
    arrayOf(
            Relation.Embedded(SnakeCase, goDeeper.First)
          , Relation.Embedded(SnakeCase, goDeeper.First / DeeplyNested.Nested)
          , Relation.Embedded(SnakeCase, goDeeper.First / DeeplyNested.Nested / WithNested.Nested)
          , Relation.Embedded(SnakeCase, goDeeper.Second)
          , Relation.Embedded(SnakeCase, goDeeper.Second / WithNullableNested.Nested, "fieldSet")
          , Relation.Embedded(SnakeCase, goDeeper.Third, "which")
          , Relation.Embedded(SnakeCase, goDeeper.Third % goDeeper.Third.type.schema.Second, "fieldSet")
    )
}


object WithNullableNested : Schema<WithNullableNested>() {
    val OwnField = "q" let string
    val Nested = "w" mut nullable(SchWithId)
    val OtherOwnField = "e" let long
}
val TableWithNullableEmbed = tableOf(WithNullableNested, "with_nullable_nested", "_id", long) {
    arrayOf(
            Relation.Embedded(SnakeCase, WithNullableNested.Nested, "nested_fields")
    )
}

object WithPartialNested : Schema<WithPartialNested>() {
    val Nested = "nested" mut partial(SchWithId)
    val OwnField = "q" let string
}
val TableWithPartialEmbed = tableOf(WithPartialNested, "with_partial_nested", "_id", long) {
    arrayOf(
            Relation.Embedded(SnakeCase, WithPartialNested.Nested, "nested_fields")
    )
}

object WithEverything : Schema<WithEverything>() {
    val Nest1 = "nest1" let nullable(partial(WithPartialNested))
    val Nest2 = "nest2" mut SchWithId
}
val TableWithEverything = tableOf(WithEverything, "with_everything", "_id", long) {
    arrayOf(
            Relation.Embedded(SnakeCase, WithEverything.Nest1, "fields"),
            Relation.Embedded(SnakeCase, WithEverything.Nest1 / WithPartialNested.Nested, "fields"),
            Relation.Embedded(SnakeCase, WithEverything.Nest2)
    )
}

val TestTables = arrayOf(
        SomeTable, TableWithId, TableWithEmbed, TableWithDeepEmbed, WeNeedToGoDeeper,
        TableWithNullableEmbed, TableWithPartialEmbed, TableWithEverything
)

val jdbcSession by lazy { // init only when requested, unused in Rololectric tests
    JdbcSession(DriverManager.getConnection("jdbc:sqlite::memory:").also { conn ->
        val stmt = conn.createStatement()
        TestTables.forEach {
            stmt.execute(SqliteDialect.createTable(it))
        }
        stmt.close()
    }, SqliteDialect)
}

fun Session.createTestRecord() =
        withTransaction {
            insert(SomeTable, SomeSchema.build {
                it[A] = "first"
                it[B] = 2
                it[C] = 3
            })
        }
