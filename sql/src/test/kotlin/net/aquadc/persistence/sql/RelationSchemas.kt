package net.aquadc.persistence.sql

import net.aquadc.persistence.extended.Tuple
import net.aquadc.persistence.extended.partial
import net.aquadc.persistence.struct.Named
import net.aquadc.persistence.struct.NamedLens
import net.aquadc.persistence.struct.PartialStruct
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.long
import net.aquadc.persistence.type.nullable
import net.aquadc.persistence.type.string
import org.junit.Assert.assertEquals
import org.junit.Test

typealias ShallowSchema = Tuple<String, DataType.Simple<String>, Long, DataType.Simple<Long>>
val shallowSchema = Tuple("a", string, "b", long)

val dupeEmbed = Tuple("a_b", string, "a", shallowSchema)

val embedSchema = Tuple("a", string, "b", shallowSchema)

val embedPartial = Tuple("a", string, "b", partial(shallowSchema))

val embedNullable = Tuple("a", string, "b", nullable(shallowSchema))

val embedNullablePartial = Tuple("a", string, "b", nullable(partial(shallowSchema)))

class RelationSchemas {

    @Test fun `no rels`() {
        val table = tableOf(shallowSchema, "zzz", "_id", long)
        assertEquals(
                arrayOf(PkLens(table), shallowSchema.First, shallowSchema.Second),
                table.columns
        )
    }
    @Test(expected = IllegalStateException::class) fun `same name as pk`() {
        tableOf(shallowSchema, "dupeName", "a", long).columns
    }

    @Test(expected = IllegalStateException::class) fun `same name as nested`() {
        tableOf(dupeEmbed, "", "a", long) {
            arrayOf(
                    Relation.Embedded(SnakeCase, dupeEmbed.Second)
            )
        }.columns
    }

    object EmptyName : Schema<EmptyName>() {
        val a = "" let string
    }
    @Test(expected = IllegalStateException::class) fun `empty name`() {
        tableOf(EmptyName, "", "id", long).columns
    }

    // primary key

    @Test fun `with id`() {
        assertEquals(
                arrayOf(SchWithId.Id, SchWithId.Value, SchWithId.MutValue),
                TableWithId.columns
        )
    }

    // embedded

    @Test(expected = NoSuchElementException::class)
    fun `rels required`() {
        tableOf(embedSchema, "zzz", "_id", long).columns
    }
    @Test fun `embed struct`() {
        val table = tableOf(embedSchema, "zzz", "_id", long) {
            arrayOf(
                    Relation.Embedded(SnakeCase, embedSchema.Second)
            )
        }
        assertEquals(arrayOf("_id", "a", "b_a", "b_b"), table.columns.names())
        assertEquals(
                arrayOf(
                        PkLens(table),
                        embedSchema.First,
                        Telescope("", embedSchema.Second, shallowSchema.First),
                        Telescope("", embedSchema.Second, shallowSchema.Second)
                ),
                table.columns
        )
    }

    @Test(expected = NoSuchElementException::class)
    fun `fieldSetCol required for partial`() {
        tableOf(embedPartial, "zzz", "_id", long) {
            arrayOf(
                    Relation.Embedded(SnakeCase, embedPartial.Second)
            )
        }.columns
    }
    @Test fun `embed partial`() {
        val table = tableOf(embedPartial, "zzz", "_id", long) {
            arrayOf(
                    Relation.Embedded(SnakeCase, embedPartial.Second, "fieldsSet")
            )
        }
        assertEquals(arrayOf("_id", "a", "b_fieldsSet", "b_a", "b_b"), table.columns.names())
        assertEquals(
                arrayOf(
                        PkLens(table),
                        embedPartial.First,
                        embedPartial.Second / FieldSetLens<ShallowSchema>("fieldsSet"),
                        Telescope("b_a", embedPartial.Second, shallowSchema.First),
                        Telescope("b_b", embedPartial.Second, shallowSchema.Second)
                ),
                table.columns
        )
    }

    @Test(expected = NoSuchElementException::class)
    fun `fieldSetCol required for nullable`() {
        tableOf(embedNullable, "zzz", "_id", long) {
            arrayOf(
                    Relation.Embedded(SnakeCase, embedNullable.Second)
            )
        }.columns
    }
    @Test fun `embed nullable`() {
        val table = tableOf(embedNullable, "zzz", "_id", long) {
            arrayOf(
                    Relation.Embedded(SnakeCase, embedNullable.Second, "nullability")
            )
        }
        assertEquals(arrayOf("_id", "a", "b_nullability", "b_a", "b_b"), table.columns.names())
        assertEquals(
                arrayOf(
                        PkLens(table),
                        embedNullable.First,
                        embedNullable.Second / FieldSetLens("nullability"),
                        Telescope("b_a", embedNullable.Second, shallowSchema.First),
                        Telescope("b_b", embedNullable.Second, shallowSchema.Second)
                ),
                table.columns
        )
    }

    @Test(expected = NoSuchElementException::class)
    fun `fieldSetCol required for partial nullable`() {
        tableOf(embedNullablePartial, "zzz", "_id", long) {
            arrayOf(
                    Relation.Embedded(SnakeCase, embedNullablePartial.Second)
            )
        }.columns
    }
    @Test fun `embed nullable partial`() {
        val table = tableOf(embedNullablePartial, "zzz", "_id", long) {
            arrayOf(
                    Relation.Embedded(SnakeCase, embedNullablePartial.Second, "fieldSetAndNullability")
            )
        }
        assertEquals(
                arrayOf("_id", "a", "b_fieldSetAndNullability", "b_a", "b_b"),
                table.columns.names()
        )
        assertEquals(
                arrayOf(
                        PkLens(table),
                        embedNullablePartial.First,
                        embedNullablePartial.Second / FieldSetLens<ShallowSchema>("fieldSetAndNullability"),
                        Telescope("b_a", embedNullablePartial.Second, shallowSchema.First),
                        Telescope("b_b", embedNullablePartial.Second, shallowSchema.Second)
                ),
                table.columns
        )
    }

    private fun Array<out Named>.names(): Array<out String> =
            mapIndexedToArray { _, it -> it.name }

    private fun assertEquals(expected: Array<out Any?>, actual: Array<out Any?>) {
        if (expected.size != actual.size) assertEquals(expected as Any, actual as Any) // fallback to JUnit impl
        expected.zip(actual).forEachIndexed { idx, (ex, ac) ->
            assertEquals("at $idx", ex, ac) // fail separately
        }
    }

}
