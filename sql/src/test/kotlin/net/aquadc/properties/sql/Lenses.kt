package net.aquadc.properties.sql

import net.aquadc.persistence.extended.buildPartial
import net.aquadc.persistence.extended.partial
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.build
import net.aquadc.persistence.struct.ofStruct
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
            with(SnakeCase) { NestedSchema.Items / SomeSchema.A },
            with(CamelCase) { NestedSchema.Items / SomeSchema.A }
    )

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
        val struct = NestedPartial.build {
            it[Item] = SomeSchema.buildPartial {  }
        }
        val value = lens.ofStruct()(struct)
        assertEquals(null, value)
    }

}
