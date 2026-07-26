package net.aquadc.persistence.struct

import net.aquadc.persistence.type.i32
import net.aquadc.persistence.type.string
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test

class SchemaPropsTest {

    @Test fun fields() =
        assertEquals(SomeSchema.A + SomeSchema.B + SomeSchema.C + SomeSchema.D, SomeSchema.allFieldSet)

    @Test fun mutableFields() =
            assertArrayEquals(
                arrayOf(SomeSchema.B, SomeSchema.C),
                SomeSchema.mapIndexed(SomeSchema.mutableFieldSet) { _, it -> it }
            )



    @Test fun ordinals() {
        SomeSchema.forEachIndexed(SomeSchema.allFieldSet) { index, fieldDef ->
            assertEquals(index, fieldDef.ordinal.toInt())
        }
    }

    @Test fun mutableOrdinals() {
        SomeSchema.forEachIndexed(SomeSchema.mutableFieldSet) { index, fieldDef ->
            assertEquals(index, fieldDef.mutableOrdinal.toInt())
        }
    }

    @Test fun immutableOrdinals() {
        SomeSchema.forEachIndexed(SomeSchema.immutableFieldSet) { index, fieldDef ->
            assertEquals(index, fieldDef.immutableOrdinal.toInt())
        }
    }

    @Test fun `field def exposes name and type`() {
        assertSame(SomeSchema, SomeSchema.A.schema)
        assertEquals("a", SomeSchema.A.name)
        assertSame(string, SomeSchema.A.type)
        assertSame(Unset, SomeSchema.A.default)
        assertEquals(10, SomeSchema.B.default)
    }

    @Test fun `field def equality includes name and type`() {
        assertNotEquals(SomeSchema.A, DifferentSchema.A)
    }

    @Test fun `field def toString includes name and type`() {
        assertEquals("let SomeSchema[#0].a: $string", SomeSchema.A.toString())
        assertEquals("mut SomeSchema[#1].b: $i32", SomeSchema.B.toString())
    }

    @Test fun `initialization order trolling`() {
        assertEquals("a1", InitTroll.run { Second.name })
        assertSame(string, InitTroll.run { Second.type })
        assertEquals("qwer", InitTroll.defaultOrElse(InitTroll.Second) { "error" })
        assertEquals(InitTroll.First + InitTroll.Second, InitTroll.allFieldSet)
    }

    @Test fun `field by name`() {
        SomeSchema.fieldByName("a", { assertSame(SomeSchema.A, it) }, { fail() })
        SomeSchema.fieldByName("z", { fail() }, { /* expected */ })
    }

}

object InitTroll : Schema<InitTroll>() {
    val First = "a".let(string, default = "qwe")
    val Second = "${First.name}1".let(First.type, default = defaultOrElse(First) { "unexpected" } + "r")
}

object DifferentSchema : Schema<DifferentSchema>() {
    val A = "different" let string
}
