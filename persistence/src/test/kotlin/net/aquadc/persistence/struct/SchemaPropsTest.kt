package net.aquadc.persistence.struct

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class SchemaPropsTest {

    @Test fun fields() =
            assertArrayEquals(arrayOf(SomeSchema.A, SomeSchema.B, SomeSchema.C), SomeSchema.fields)

    @Test fun fieldsByName() =
            assertEquals(mapOf("a" to SomeSchema.A, "b" to SomeSchema.B, "c" to SomeSchema.C), SomeSchema.fieldsByName)

    @Test fun mutableFields() =
            assertArrayEquals(arrayOf(SomeSchema.C), SomeSchema.mutableFields)



    @Test fun ordinals() {
        SomeSchema.fields.forEachIndexed { index, fieldDef ->
            assertEquals(index, fieldDef.ordinal.toInt())
        }
    }

    @Test fun mutableOrdinals() {
        SomeSchema.mutableFields.forEachIndexed { index, fieldDef ->
            assertEquals(index, fieldDef.mutableOrdinal.toInt())
        }
    }

    @Test fun immutableOrdinals() {
        SomeSchema.immutableFields.forEachIndexed { index, fieldDef ->
            assertEquals(index, fieldDef.immutableOrdinal.toInt())
        }
    }

}
