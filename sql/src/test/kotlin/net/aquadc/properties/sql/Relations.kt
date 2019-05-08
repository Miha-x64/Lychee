package net.aquadc.properties.sql

import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.struct.StructSnapshot
import net.aquadc.persistence.struct.build
import net.aquadc.persistence.struct.copy
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
                it[Nested] = SchWithId.build {
                    it[Id] = 100500
                    it[Value] = "200700"
                }
                it[OtherOwnField] = 16_000_000_000
            })

            // read uncommitted
            assertEquals("qwe", rec[WithNested.OwnField])
            assertEquals(100500, rec[WithNested.Nested][SchWithId.Id])
            assertEquals("200700", rec[WithNested.Nested][SchWithId.Value])
            assertEquals(16_000_000_000, rec[WithNested.OtherOwnField])

            rec
        }

        assertEquals("qwe", record[WithNested.OwnField])
        assertEquals(100500, record[WithNested.Nested][SchWithId.Id])
        assertEquals("200700", record[WithNested.Nested][SchWithId.Value])
        assertEquals("", record[WithNested.Nested][SchWithId.MutValue])
        assertEquals(16_000_000_000, record[WithNested.OtherOwnField])

        val emb = record[WithNested.Nested]
        session.withTransaction {
            record[WithNested.Nested] = SchWithId.build {
                it[Id] = 100500
                it[Value] = "200700"
                it[MutValue] = "mutated"
            }

            assertEquals("uncommitted should be visible through mut col", "mutated", emb[SchWithId.MutValue])
        }

        val newEmb = record[WithNested.Nested]
        assertNotSame(newEmb, emb)
        assertEquals(100500, newEmb[SchWithId.Id])
        assertEquals("200700", newEmb[SchWithId.Value])
        assertEquals("mutated", newEmb[SchWithId.MutValue])
    }

    @Test fun `observing embed`() {
        val rec = session.withTransaction {
            insert(TableWithEmbed, WithNested.build {
                it[OwnField] = "qwe"
                it[Nested] = SchWithId.build {
                    it[Id] = 100500
                    it[Value] = "200700"
                }
                it[OtherOwnField] = 16_000_000_000
            })
        }

        var called = 0
        rec.prop(WithNested.Nested).map(SchWithId.Value).distinct(Objectz.Same).addUnconfinedChangeListener { old, new ->
            assertEquals("200700", old)
            assertEquals("200701", new)
            called = 1
        }

        var oldNest: Struct<SchWithId>? = null
        var newNest: Struct<SchWithId>? = null
        rec.prop(WithNested.Nested).addUnconfinedChangeListener { old, new ->
            if (oldNest === null) oldNest = StructSnapshot(old)
            newNest = new
        } // skip bouncing, may have several updates

        session.withTransaction {
            rec[WithNested.Nested] = SchWithId.build {
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

    @Test fun `deep embed`() {
        val rec = session.withTransaction {
            insert(TableWithDeepEmbed, DeeplyNested.build {
                it[OwnField] = "something"
                it[Nested] = WithNested.build {
                    it[OwnField] = "whatever"
                    it[Nested] = SchWithId.build {
                        it[Id] = -1
                        it[Value] = "zzz"
                        it[MutValue] = "hey"
                    }
                    it[OtherOwnField] = 111
                }
            })
        }

        var called = 0
        rec.prop(DeeplyNested.Nested)
                .map(WithNested.Nested)
                .map(SchWithId.MutValue)
                .distinct(dropIfValues = Objectz.Same)
                .addUnconfinedChangeListener { old, new ->
                    assertEquals("hey", old)
                    assertEquals("hi", new)
                    called++
                }

        session.withTransaction {
            rec[DeeplyNested.Nested] = rec[DeeplyNested.Nested].copy {
                it[Nested] = it[Nested].copy {
                    it[MutValue] = "hi"
                }
            }
        }

        assertEquals(1, called)
    }

    @Test fun `we need to go deeper`() {
        val f = DeeplyNested.build {
            it[OwnField] = "f1"
            it[Nested] = WithNested.build {
                it[OwnField] = "f2.1"
                it[Nested] = SchWithId.build {
                    it[Id] = 0xF_2_2_1
                    it[Value] = "f2.2.2"
                    it[MutValue] = "f2.2.3"
                }
                it[OtherOwnField] = 0xF_2_3
            }
        }
        val s = DeeplyNested.build {
            it[OwnField] = "s1"
            it[Nested] = WithNested.build {
                it[OwnField] = "s2.1"
                it[Nested] = SchWithId.build {
                    it[Id] = 0x5_2_2_1
                    it[Value] = "s2.2.2"
                    it[MutValue] = "s2.2.3"
                }
                it[OtherOwnField] = 0x5_2_3
            }
        }

        val rec = session.withTransaction {
            insert(WeNeedTOGoDeeper, GoDeeper.build {
                it[First] = f
                it[Second] = s
            })
        }

        assertEquals(f, rec[GoDeeper.First])
        assertEquals(s, rec[GoDeeper.Second])
    }

}
