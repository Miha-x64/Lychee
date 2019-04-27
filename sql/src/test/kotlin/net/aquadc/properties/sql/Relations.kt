package net.aquadc.properties.sql

import net.aquadc.persistence.extended.partial
import net.aquadc.persistence.struct.Lens
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.type.long
import net.aquadc.persistence.type.nullable
import net.aquadc.persistence.type.string
import org.junit.Assert.assertEquals
import org.junit.Test


class Relations {

    object ShallowSchema : Schema<ShallowSchema>() {
        val A = "a" let string
        val B = "b" mut long
    }
    @Test fun `no rels`() {
        assertEquals(
                listOf(
                        Pair("_id", long) to Relation.PrimaryKey,
                        ShallowSchema.A to null,
                        ShallowSchema.B to null
                ),
                SimpleTable(ShallowSchema, "zzz", "_id", long).columns
        )
    }

    // primary key

    @Test fun `with id`() {
        assertEquals(
                listOf(
                        SchWithId.Id to Relation.PrimaryKey,
                        SchWithId.Value to null
                ),
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
        val cols = object : SimpleTable<EmbedSchema, Long>(EmbedSchema, "zzz", "_id", long) {
            override fun relations(): List<Relation<EmbedSchema, Lens<EmbedSchema, *>>> = listOf(
                    Relation.Embedded(SnakeLensFactory, EmbedSchema.B)
            )
        }.columns
        assertEquals(
                listOf("_id", "a", "b_a", "b_b"),
                cols.names()
        )
        assertEquals(
                listOf(
                        Pair("_id", long) to Relation.PrimaryKey,
                        EmbedSchema.A to null,
                        Telescope("", EmbedSchema.B, ShallowSchema.A) to null,
                        Telescope("", EmbedSchema.B, ShallowSchema.B) to null
                ),
                cols
        )
    }

    object EmbedPartial : Schema<EmbedPartial>() {
        val A = "a" let string
        val B = "b" let partial(ShallowSchema)
    }
    @Test(expected = NoSuchElementException::class)
    fun `fieldSetCol required for partial`() {
        object : SimpleTable<EmbedPartial, Long>(EmbedPartial, "zzz", "_id", long) {
            override fun relations(): List<Relation<EmbedPartial, Lens<EmbedPartial, *>>> = listOf(
                    Relation.Embedded(SnakeLensFactory, EmbedPartial.B)
            )
        }.columns
    }
    @Test fun `embed partial`() {
        val cols = object : SimpleTable<EmbedPartial, Long>(EmbedPartial, "zzz", "_id", long) {
            override fun relations(): List<Relation<EmbedPartial, Lens<EmbedPartial, *>>> = listOf(
                    Relation.Embedded(SnakeLensFactory, EmbedPartial.B, "fieldsSet")
            )
        }.columns
        assertEquals(
                listOf("_id", "a", "fieldsSet", "b_a", "b_b"),
                cols.names()
        )
        assertEquals(
                listOf(
                        Pair("_id", long) to Relation.PrimaryKey,
                        EmbedPartial.A to null,
                        Pair("fieldsSet", long) to null,
                        Telescope("", EmbedPartial.B, ShallowSchema.A) to null,
                        Telescope("", EmbedPartial.B, ShallowSchema.B) to null
                ),
                cols
        )
    }

    object EmbedNullable : Schema<EmbedNullable>() {
        val A = "a" let string
        val B = "b" let nullable(ShallowSchema)
    }
    @Test(expected = NoSuchElementException::class)
    fun `fieldSetCol required for nullable`() {
        object : SimpleTable<EmbedNullable, Long>(EmbedNullable, "zzz", "_id", long) {
            override fun relations(): List<Relation<EmbedNullable, Lens<EmbedNullable, *>>> = listOf(
                    Relation.Embedded(SnakeLensFactory, EmbedNullable.B)
            )
        }.columns
    }
    @Test fun `embed nullable`() {
        val cols = object : SimpleTable<EmbedNullable, Long>(EmbedNullable, "zzz", "_id", long) {
            override fun relations(): List<Relation<EmbedNullable, Lens<EmbedNullable, *>>> = listOf(
                    Relation.Embedded(SnakeLensFactory, EmbedNullable.B, "nullability")
            )
        }.columns
        assertEquals(
                listOf("_id", "a", "nullability", "b_a", "b_b"),
                cols.names()
        )
        assertEquals(
                listOf(
                        Pair("_id", long) to Relation.PrimaryKey,
                        EmbedNullable.A to null,
                        Pair("nullability", nullableLong) to null,
                        Telescope("", EmbedNullable.B, ShallowSchema.A) to null,
                        Telescope("", EmbedNullable.B, ShallowSchema.B) to null
                ),
                cols
        )
    }

    object EmbedNullablePartial : Schema<EmbedNullablePartial>() {
        val A = "a" let string
        val B = "b" let nullable(partial(ShallowSchema))
    }
    @Test(expected = NoSuchElementException::class)
    fun `fieldSetCol required for partial nullable`() {
        object : SimpleTable<EmbedNullablePartial, Long>(EmbedNullablePartial, "zzz", "_id", long) {
            override fun relations(): List<Relation<EmbedNullablePartial, Lens<EmbedNullablePartial, *>>> = listOf(
                    Relation.Embedded(SnakeLensFactory, EmbedNullablePartial.B)
            )
        }.columns
    }
    @Test fun `embed nullable partial`() {
        val cols = object : SimpleTable<EmbedNullablePartial, Long>(EmbedNullablePartial, "zzz", "_id", long) {
            override fun relations(): List<Relation<EmbedNullablePartial, Lens<EmbedNullablePartial, *>>> = listOf(
                    Relation.Embedded(SnakeLensFactory, EmbedNullablePartial.B, "fieldSetAndNullability")
            )
        }.columns
        assertEquals(
                listOf("_id", "a", "fieldSetAndNullability", "b_a", "b_b"),
                cols.names()
        )
        assertEquals(
                listOf(
                        Pair("_id", long) to Relation.PrimaryKey,
                        EmbedNullablePartial.A to null,
                        Pair("fieldSetAndNullability", nullableLong) to null,
                        Telescope("", EmbedNullablePartial.B, ShallowSchema.A) to null,
                        Telescope("", EmbedNullablePartial.B, ShallowSchema.B) to null
                ),
                cols
        )
    }

    private fun List<Pair<Column, Relation<*, *>?>>.names(): List<Any?> {
        return map { (col, _) ->
            if (col is Pair<*, *>) col.first else if (col is Lens<*, *>) col.name else error("")
        }
    }

}
