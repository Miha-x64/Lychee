package net.aquadc.persistence

import net.aquadc.persistence.struct.build
import net.aquadc.persistence.struct.transaction
import net.aquadc.properties.addUnconfinedChangeListener
import net.aquadc.properties.persistence.ObservableStruct
import net.aquadc.properties.persistence.snapshots
import net.aquadc.properties.testing.SomeSchema
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test fun transactional() {
        val initial = ObservableStruct(SomeSchema.build {
            it[A] = "first"
            it[B] = 2
            it[C] = 3
        }, false)

        var called = false
        val listener = { old: Long, new: Long ->
            assertEquals(3L, old)
            assertEquals(10L, new)
            called = true
        }
        (initial prop SomeSchema.C).addUnconfinedChangeListener(listener)

        val trans = initial.transactional()

        // normal transaction
        trans.transaction {
            it[C] = 10L
            assertFalse(called)
        }
        assertTrue(called)

        // rollback transaction
        called = false
        try {
            trans.transaction {
                it[C] = 100L
                throw NoWhenBranchMatchedException()
            }
        } catch (ignored: NoWhenBranchMatchedException) {}

        assertFalse(called)
        assertEquals(10L, initial[SomeSchema.C])
    }

}
