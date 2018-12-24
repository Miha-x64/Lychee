package net.aquadc.persistence

import net.aquadc.persistence.struct.build
import net.aquadc.properties.persistence.ObservableStruct
import net.aquadc.properties.persistence.snapshots
import org.junit.Assert.assertEquals
import org.junit.Test

class PropStructTest {

    @Test fun snapshots() {
        val initial = SomeSchema.build {
            it[A] = "first"
            it[B] = 2
            it[C] = 3
        }

        val struct = ObservableStruct(initial, false)
        assertEquals(initial, struct)

        val snaps = struct.snapshots()
        assertEquals(initial, snaps.value)

        struct[SomeSchema.C] = 10
        assertEquals(SomeSchema.build {
            it[A] = "first"
            it[B] = 2
            it[C] = 10
        }, snaps.value)
    }

}
