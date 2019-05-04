package net.aquadc.properties.sql

import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.struct.StructSnapshot
import net.aquadc.persistence.struct.build
import net.aquadc.properties.addUnconfinedChangeListener
import net.aquadc.properties.distinct
import net.aquadc.properties.function.Objectz
import net.aquadc.properties.map
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Test


class Relations {

    @Test fun embed() {
        val record = session.withTransaction {
            val rec = insert(TableWithEmbed, WithNested.build {
                it[OwnField] = "qwe"
                it[Embedded] = SchWithId.build {
                    it[Id] = 100500
                    it[Value] = "200700"
                }
                it[OtherOwnField] = 16_000_000_000
            })

            // read uncommitted
            assertEquals("qwe", rec[WithNested.OwnField])
            assertEquals(100500, rec[WithNested.Embedded][SchWithId.Id])
            assertEquals("200700", rec[WithNested.Embedded][SchWithId.Value])
            assertEquals(16_000_000_000, rec[WithNested.OtherOwnField])

            rec
        }

        assertEquals("qwe", record[WithNested.OwnField])
        assertEquals(100500, record[WithNested.Embedded][SchWithId.Id])
        assertEquals("200700", record[WithNested.Embedded][SchWithId.Value])
        assertEquals("", record[WithNested.Embedded][SchWithId.MutValue])
        assertEquals(16_000_000_000, record[WithNested.OtherOwnField])

        val emb = record[WithNested.Embedded]
        session.withTransaction {
            record[WithNested.Embedded] = SchWithId.build {
                it[Id] = 100500
                it[Value] = "200700"
                it[MutValue] = "mutated"
            }

            assertEquals("uncommitted should be visible through mut col", "mutated", emb[SchWithId.MutValue])
        }

        val newEmb = record[WithNested.Embedded]
        assertNotSame(newEmb, emb)
        assertEquals(100500, newEmb[SchWithId.Id])
        assertEquals("200700", newEmb[SchWithId.Value])
        assertEquals("mutated", newEmb[SchWithId.MutValue])
    }

    @Test fun `observing embed`() {
        val rec = session.withTransaction {
            insert(TableWithEmbed, WithNested.build {
                it[OwnField] = "qwe"
                it[Embedded] = SchWithId.build {
                    it[Id] = 100500
                    it[Value] = "200700"
                }
                it[OtherOwnField] = 16_000_000_000
            })
        }

        var called = 0
        rec.prop(WithNested.Embedded).map(SchWithId.Value).distinct(Objectz.Same).addUnconfinedChangeListener { old, new ->
            assertEquals("200700", old)
            assertEquals("200701", new)
            called = 1
        }

        var oldNest: Struct<SchWithId>? = null
        var newNest: Struct<SchWithId>? = null
        rec.prop(WithNested.Embedded).addUnconfinedChangeListener { old, new ->
            if (oldNest === null) oldNest = StructSnapshot(old)
            newNest = new
        } // skip bouncing, may have several updates

        session.withTransaction {
            rec[WithNested.Embedded] = SchWithId.build {
                it[Id] = 100500
                it[Value] = "200701"
            }
        }

        assertEquals(1, called)

        assertEquals(SchWithId.build {
            it[Id] = 100500
            it[Value] = "200700"
        }, oldNest)

        assertEquals(SchWithId.build {
            it[Id] = 100500
            it[Value] = "200701"
        }, newNest)
    }

    // TODO: test deeper nesting

}
