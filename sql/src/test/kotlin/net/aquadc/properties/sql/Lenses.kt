package net.aquadc.properties.sql

import net.aquadc.persistence.struct.Schema
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test


class Lenses {

    object DeeplyNestedSchema : Schema<DeeplyNestedSchema>() {
        val Items = "items" let NestedSchema
    }

    object NestedSchema : Schema<NestedSchema>() {
        val Items = "items" let SomeSchema
    }

    @Test fun `eq despite names`() = assertEquals(
            with(SnakeLensFactory) { NestedSchema.Items / SomeSchema.A },
            with(CamelLensFactory) { NestedSchema.Items / SomeSchema.A }
    )

    @Test fun `nest eq`() = with(SnakeLensFactory) {
        assertEquals(
                DeeplyNestedSchema.Items / NestedSchema.Items / SomeSchema.A,
                DeeplyNestedSchema.Items / (NestedSchema.Items / SomeSchema.A)
        )
        assertEquals(
                (DeeplyNestedSchema.Items / NestedSchema.Items / SomeSchema.A).hashCode(),
                (DeeplyNestedSchema.Items / (NestedSchema.Items / SomeSchema.A)).hashCode()
        )
    }

    @Test fun ne() = with(SnakeLensFactory) {
        assertNotEquals(
                NestedSchema.Items / SomeSchema.A,
                NestedSchema.Items / SomeSchema.B
        )
    }

    @Test fun elements2() {
        val lens = with(SnakeLensFactory) { NestedSchema.Items / SomeSchema.A }
        assertEquals(NestedSchema.Items, lens[0])
        assertEquals(SomeSchema.A, lens[1])
    }

    @Test fun elements3() {
        val lens = with(SnakeLensFactory) { DeeplyNestedSchema.Items / NestedSchema.Items / SomeSchema.A }
        assertEquals(DeeplyNestedSchema.Items, lens[0])
        assertEquals(NestedSchema.Items, lens[1])
        assertEquals(SomeSchema.A, lens[2])
    }

}
