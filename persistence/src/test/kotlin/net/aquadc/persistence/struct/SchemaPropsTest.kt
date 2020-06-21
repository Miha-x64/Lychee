package net.aquadc.persistence.struct

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class SchemaPropsTest {

    @Test fun fields() =
        Unit//assertEquals((SomeSchema.A + SomeSchema.B + SomeSchema.C + SomeSchema.D).bitSet, SomeSchema.allFieldSet.bitSet)

    @Test fun mutableFields() =
            assertArrayEquals(
                arrayOf(SomeSchema.B, SomeSchema.C),
                SomeSchema.mapIndexed(SomeSchema.mutableFieldSet) { _, it -> it }
            )



    @Test fun ordinals() =
        SomeSchema.forEachIndexed(SomeSchema.allFieldSet) { index, fieldDef ->
            assertEquals(index, fieldDef.ordinal)
        }

    @Test fun mutableOrdinals() =
        SomeSchema.forEachIndexed_(SomeSchema.mutableFieldSet) { index, fieldDef ->
            assertEquals(index, fieldDef.mutableOrdinal)
        }

    @Test fun immutableOrdinals() =
        SomeSchema.forEachIndexed_(SomeSchema.immutableFieldSet) { index, fieldDef ->
            assertEquals(index, fieldDef.immutableOrdinal)
        }

}
