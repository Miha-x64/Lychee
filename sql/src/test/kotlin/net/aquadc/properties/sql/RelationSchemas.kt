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
                arrayOf(PkLens(table), ShallowSchema.A, ShallowSchema.B),
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
            override fun relations(): Array<Relation<DupeEmbed, Long, *>> = arrayOf(
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
                arrayOf(SchWithId.Id, SchWithId.Value, SchWithId.MutValue),
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
            override fun relations(): Array<Relation<EmbedSchema, Long, *>> = arrayOf(
                    Relation.Embedded(SnakeCase, EmbedSchema.B)
            )
        }
        assertEquals(arrayOf("_id", "a", "b_a", "b_b"), table.columns.names())
        assertEquals(
                arrayOf(
                        PkLens(table),
                        EmbedSchema.A,
                        Telescope("", EmbedSchema.B, ShallowSchema.A),
                        Telescope("", EmbedSchema.B, ShallowSchema.B)
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
            override fun relations(): Array<Relation<EmbedPartial, Long, *>> = arrayOf(
                    Relation.Embedded(SnakeCase, EmbedPartial.B)
            )
        }.columns
    }
    @Test fun `embed partial`() {
        val table: SimpleTable<EmbedPartial, Long> = object : SimpleTable<EmbedPartial, Long>(EmbedPartial, "zzz", "_id", long) {
            override fun relations(): Array<Relation<EmbedPartial, Long, *>> = arrayOf(
                    Relation.Embedded(SnakeCase, EmbedPartial.B, "fieldsSet")
            )
        }
        assertEquals(arrayOf("_id", "a", "b_fieldsSet", "b_a", "b_b"), table.columns.names())
        assertEquals(
                arrayOf(
                        PkLens(table),
                        EmbedPartial.A,
                        EmbedPartial.B / FieldSetLens("fieldsSet"),
                        Telescope("b_a", EmbedPartial.B, ShallowSchema.A),
                        Telescope("b_b", EmbedPartial.B, ShallowSchema.B)
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
            override fun relations(): Array<Relation<EmbedNullable, Long, *>> = arrayOf(
                    Relation.Embedded(SnakeCase, EmbedNullable.B)
            )
        }.columns
    }
    @Test fun `embed nullable`() {
        val table = object : SimpleTable<EmbedNullable, Long>(EmbedNullable, "zzz", "_id", long) {
            override fun relations(): Array<Relation<EmbedNullable, Long, *>> = arrayOf(
                    Relation.Embedded(SnakeCase, EmbedNullable.B, "nullability")
            )
        }
        assertEquals(arrayOf("_id", "a", "b_nullability", "b_a", "b_b"), table.columns.names())
        assertEquals(
                arrayOf(
                        PkLens(table),
                        EmbedNullable.A,
                        EmbedNullable.B / FieldSetLens("nullability"),
                        Telescope("b_a", EmbedNullable.B, ShallowSchema.A),
                        Telescope("b_b", EmbedNullable.B, ShallowSchema.B)
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
            override fun relations(): Array<Relation<EmbedNullablePartial, Long, *>> = arrayOf(
                    Relation.Embedded(SnakeCase, EmbedNullablePartial.B)
            )
        }.columns
    }
    @Test fun `embed nullable partial`() {
        val table = object : SimpleTable<EmbedNullablePartial, Long>(EmbedNullablePartial, "zzz", "_id", long) {
            override fun relations(): Array<Relation<EmbedNullablePartial, Long, *>> = arrayOf(
                    Relation.Embedded(SnakeCase, EmbedNullablePartial.B, "fieldSetAndNullability")
            )
        }
        assertEquals(
                arrayOf("_id", "a", "b_fieldSetAndNullability", "b_a", "b_b"),
                table.columns.names()
        )
        assertEquals(
                arrayOf(
                        PkLens(table),
                        EmbedNullablePartial.A,
                        EmbedNullablePartial.B / FieldSetLens("fieldSetAndNullability"),
                        Telescope("b_a", EmbedNullablePartial.B, ShallowSchema.A),
                        Telescope("b_b", EmbedNullablePartial.B, ShallowSchema.B)
                ),
                table.columns
        )
    }

    private fun Array<out NamedLens<*, *, *>>.names(): Array<out String> =
            mapIndexedToArray { _, it -> it.name }

    private fun assertEquals(expected: Array<out Any?>, actual: Array<out Any?>) {
        if (expected.size != actual.size) assertEquals(expected as Any, actual as Any) // fallback to JUnit impl
        expected.zip(actual).forEachIndexed { idx, (ex, ac) ->
            assertEquals("at $idx", ex, ac) // fail separately
        }
    }

    private inline fun <T, reified R> Array<T>.mapIndexedToArray(transform: (Int, T) -> R): Array<R> {
        val array = arrayOfNulls<R>(size)
        for (i in indices) {
            array[i] = transform(i, this[i])
        }
        @Suppress("UNCHECKED_CAST") // now it's filled with items and not thus not nullable
        return array as Array<R>
    }

}
