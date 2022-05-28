package net.aquadc.persistence.sql

import net.aquadc.persistence.extended.Partial
import net.aquadc.persistence.extended.tuple.Tuple4
import net.aquadc.persistence.extended.partial
import net.aquadc.persistence.extended.tuple.Tuple
import net.aquadc.persistence.extended.tuple.times
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.invoke
import net.aquadc.persistence.type.nullable
import net.aquadc.persistence.type.string
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test


class LensesTest {

    object DeeplyNestedSchema : Schema<DeeplyNestedSchema>() {
        val Items = "items" let NestedSchema
    }

    object NestedSchema : Schema<NestedSchema>() {
        val Items = "items" let SomeSchema
    }

    @Test fun `eq despite names`() = assertEquals(
            SnakeCase.concatErased(NestedSchema, SomeSchema, NestedSchema.Items, SomeSchema.A),
            CamelCase.concatErased(NestedSchema, SomeSchema, NestedSchema.Items, SomeSchema.A)
    )

    @Test fun camelCase() {
        assertEquals("camelCase", CamelCase.concatNames("camel", "case"))
        assertEquals("верблюжийРегистр", CamelCase.concatNames("верблюжий", "регистр"))
        assertEquals("blah\uD801\uDC00blah", CamelCase.concatNames("blah", "\uD801\uDC28blah"))
    }

    @Test fun `nest eq`() {
        assertEquals(
                DeeplyNestedSchema.Items / NestedSchema.Items / SomeSchema.A,
                DeeplyNestedSchema.Items / (NestedSchema.Items / SomeSchema.A)
        )
        assertEquals(
                (DeeplyNestedSchema.Items / NestedSchema.Items / SomeSchema.A).hashCode(),
                (DeeplyNestedSchema.Items / (NestedSchema.Items / SomeSchema.A)).hashCode()
        )
    }

    @Test fun ne() {
        assertNotEquals(
                NestedSchema.Items / SomeSchema.A,
                NestedSchema.Items / SomeSchema.B
        )
    }

    @Test fun elements2() {
        val lens = NestedSchema.Items / SomeSchema.A
        assertEquals(NestedSchema.Items, lens[0])
        assertEquals(SomeSchema.A, lens[1])
    }

    @Test fun elements3() {
        val lens = DeeplyNestedSchema.Items / NestedSchema.Items / SomeSchema.A
        assertEquals(DeeplyNestedSchema.Items, lens[0])
        assertEquals(NestedSchema.Items, lens[1])
        assertEquals(SomeSchema.A, lens[2])
    }

    object NestedPartial : Schema<NestedPartial>() {
        val Item = "" let partial(SomeSchema)
    }
    @Test fun partial() {
        val lens = NestedPartial.Item / SomeSchema.A
        val struct = NestedPartial {
            it[Item] = SomeSchema.Partial {  }
        }
        val value = lens(struct)
        assertEquals(null, value)
    }

    @Test fun `struct nullability propagation`() {
        val nested = string * nullable(string)
        val schema = Tuple4(
                "1", nested,
                "2", nullable(nested),
                "3", partial(nested),
                "4", nullable(partial(nested))
        )
        assertEquals(string, (schema.First / nested.First).type(schema))
        assertEquals(nullable(string), (schema.First / nested.Second).type(schema))
        assertEquals(nullable(string), (schema.Second / nested.First).type(schema))
        assertEquals(nullable(string), (schema.Second / nested.Second).type(schema))
        assertEquals(nullable(string), (schema.Third / nested.First).type(schema))
        assertEquals(nullable(string), (schema.Third / nested.Second).type(schema))
        assertEquals(nullable(string), (schema.Fourth / nested.First).type(schema))
        assertEquals(nullable(string), (schema.Fourth / nested.Second).type(schema))
    }

    @Test fun `non-struct nullability propagation`() {
        val schema = schemaWithNullableEither
        assertEquals(nullable(string), (schema.First % (schema.First.type(schema).schema as Tuple).First).type(schema))
        assertEquals(nullable(string), (schema.First % (schema.First.type(schema).schema as Tuple).Second).type(schema))
        assertEquals(nullable(string), (schema.Second % (schema.First.type(schema).schema as Tuple).First).type(schema))
        assertEquals(nullable(string), (schema.Second % (schema.First.type(schema).schema as Tuple).Second).type(schema))
    }

}
