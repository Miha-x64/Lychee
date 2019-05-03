package net.aquadc.properties.sql

import net.aquadc.persistence.extended.partial
import net.aquadc.persistence.struct.NamedLens
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.type.long
import net.aquadc.persistence.type.nullable
import net.aquadc.persistence.type.string
import org.junit.Assert.assertEquals
import org.junit.Test


class RelationSchemas {

    object ShallowSchema : Schema<ShallowSchema>() {
        val A = "a" let string
        val B = "b" mut long
    }
    @Test fun `no rels`() {
        val table = SimpleTable(ShallowSchema, "zzz", "_id", long)
        assertEquals(
                listOf(PkLens(table), ShallowSchema.A, ShallowSchema.B),
                table.columns
        )
    }
    @Test(expected = IllegalStateException::class) fun `same name as pk`() {
        SimpleTable(ShallowSchema, "dupeName", "a", long).columns
    }

    object DupeEmbed : Schema<DupeEmbed>() {
        val a = "a_b" let string
        val b = "a" let ShallowSchema
    }
    @Test(expected = IllegalStateException::class) fun `same name as nested`() {
        object : SimpleTable<DupeEmbed, Long>(DupeEmbed, "", "a", long) {
            override fun relations(): List<Relation<DupeEmbed, Long, *>> = listOf(
                    Relation.Embedded(SnakeCase, DupeEmbed.b)
            )
        }.columns
    }

    object EmptyName : Schema<EmptyName>() {
        val a = "" let string
    }
    @Test(expected = IllegalStateException::class) fun `empty name`() {
        SimpleTable(EmptyName, "", "id", long).columns
    }

    // primary key

    @Test fun `with id`() {
        assertEquals(
                listOf(SchWithId.Id, SchWithId.Value, SchWithId.MutValue),
                TableWithId.columns
        )
    }

    // embedded

    object EmbedSchema : Schema<EmbedSchema>() {
        val A = "a" let string
        val B = "b" let ShallowSchema
    }
    @Test(expected = NoSuchElementException::class)
    fun `rels required`() {
        SimpleTable(EmbedSchema, "zzz", "_id", long).columns
    }
    @Test fun `embed struct`() {
        val table = object : SimpleTable<EmbedSchema, Long>(EmbedSchema, "zzz", "_id", long) {
            override fun relations(): List<Relation<EmbedSchema, Long, *>> = listOf(
                    Relation.Embedded(SnakeCase, EmbedSchema.B)
            )
        }
        assertEquals(listOf("_id", "a", "b_a", "b_b"), table.columns.names())
        assertEquals(
                listOf(
                        PkLens(table),
                        EmbedSchema.A,
                        Telescope1("", EmbedSchema.B, ShallowSchema.A),
                        Telescope1("", EmbedSchema.B, ShallowSchema.B)
                ),
                table.columns
        )
    }

    object EmbedPartial : Schema<EmbedPartial>() {
        val A = "a" let string
        val B = "b" let partial(ShallowSchema)
    }
    @Test(expected = NoSuchElementException::class)
    fun `fieldSetCol required for partial`() {
        object : SimpleTable<EmbedPartial, Long>(EmbedPartial, "zzz", "_id", long) {
            override fun relations(): List<Relation<EmbedPartial, Long, *>> = listOf(
                    Relation.Embedded(SnakeCase, EmbedPartial.B)
            )
        }.columns
    }
    @Test fun `embed partial`() {
        val table: SimpleTable<EmbedPartial, Long> = object : SimpleTable<EmbedPartial, Long>(EmbedPartial, "zzz", "_id", long) {
            override fun relations(): List<Relation<EmbedPartial, Long, *>> = listOf(
                    Relation.Embedded(SnakeCase, EmbedPartial.B, "fieldsSet")
            )
        }
        assertEquals(listOf("_id", "a", "fieldsSet", "b_a", "b_b"), table.columns.names())
        assertEquals(
                listOf(
                        PkLens(table),
                        EmbedPartial.A,
                        SyntheticColLens(table,"fieldsSet", EmbedPartial.B, false),
                        Telescope1("b_a", EmbedPartial.B, ShallowSchema.A),
                        Telescope1("b_b", EmbedPartial.B, ShallowSchema.B)
                ),
                table.columns
        )
    }

    object EmbedNullable : Schema<EmbedNullable>() {
        val A = "a" let string
        val B = "b" let nullable(ShallowSchema)
    }
    @Test(expected = NoSuchElementException::class)
    fun `fieldSetCol required for nullable`() {
        object : SimpleTable<EmbedNullable, Long>(EmbedNullable, "zzz", "_id", long) {
            override fun relations(): List<Relation<EmbedNullable, Long, *>> = listOf(
                    Relation.Embedded(SnakeCase, EmbedNullable.B)
            )
        }.columns
    }
    @Test fun `embed nullable`() {
        val table = object : SimpleTable<EmbedNullable, Long>(EmbedNullable, "zzz", "_id", long) {
            override fun relations(): List<Relation<EmbedNullable, Long, *>> = listOf(
                    Relation.Embedded(SnakeCase, EmbedNullable.B, "nullability")
            )
        }
        assertEquals(listOf("_id", "a", "nullability", "b_a", "b_b"), table.columns.names())
        assertEquals(
                listOf(
                        PkLens(table),
                        EmbedNullable.A,
                        SyntheticColLens(table, "nullability", EmbedNullable.B, true),
                        Telescope0("b_a", EmbedNullable.B, ShallowSchema.A),
                        Telescope0("b_b", EmbedNullable.B, ShallowSchema.B)
                ),
                table.columns
        )
    }

    object EmbedNullablePartial : Schema<EmbedNullablePartial>() {
        val A = "a" let string
        val B = "b" let nullable(partial(ShallowSchema))
    }
    @Test(expected = NoSuchElementException::class)
    fun `fieldSetCol required for partial nullable`() {
        object : SimpleTable<EmbedNullablePartial, Long>(EmbedNullablePartial, "zzz", "_id", long) {
            override fun relations(): List<Relation<EmbedNullablePartial, Long, *>> = listOf(
                    Relation.Embedded(SnakeCase, EmbedNullablePartial.B)
            )
        }.columns
    }
    @Test fun `embed nullable partial`() {
        val table = object : SimpleTable<EmbedNullablePartial, Long>(EmbedNullablePartial, "zzz", "_id", long) {
            override fun relations(): List<Relation<EmbedNullablePartial, Long, *>> = listOf(
                    Relation.Embedded(SnakeCase, EmbedNullablePartial.B, "fieldSetAndNullability")
            )
        }
        assertEquals(
                listOf("_id", "a", "fieldSetAndNullability", "b_a", "b_b"),
                table.columns.names()
        )
        assertEquals(
                listOf(
                        PkLens(table),
                        EmbedNullablePartial.A,
                        SyntheticColLens(table, "fieldSetAndNullability", EmbedNullablePartial.B, false),
                        Telescope0("b_a", EmbedNullablePartial.B, ShallowSchema.A),
                        Telescope0("b_b", EmbedNullablePartial.B, ShallowSchema.B)
                ),
                table.columns
        )
    }

    private fun List<NamedLens<*, *, *>>.names(): List<String> =
            map(NamedLens<*, *, *>::name)

    private fun assertEquals(expected: List<Any?>, actual: List<Any?>) {
        if (expected.size != actual.size) assertEquals(expected as Any, actual as Any) // fallback to JUnit impl
        expected.zip(actual).forEachIndexed { idx, (ex, ac) ->
            assertEquals("at $idx", ex, ac) // fail separately
        }
    }

}
