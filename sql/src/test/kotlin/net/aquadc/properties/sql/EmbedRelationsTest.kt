package net.aquadc.properties.sql

import net.aquadc.persistence.extended.buildPartial
import net.aquadc.persistence.extended.copy
import net.aquadc.persistence.extended.getOrDefault
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.struct.StructSnapshot
import net.aquadc.persistence.struct.asFieldSet
import net.aquadc.persistence.struct.build
import net.aquadc.persistence.struct.copy
import net.aquadc.persistence.struct.ofStruct
import net.aquadc.properties.addUnconfinedChangeListener
import net.aquadc.properties.distinct
import net.aquadc.properties.function.Objectz
import net.aquadc.properties.map
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test


class EmbedRelationsTest {

    @Test fun embed() {
        val rec = session.withTransaction {
            val rec = insert(TableWithEmbed, WithNested.build {
                it[OwnField] = "qwe"
                it[Nested] = SchWithId.build {
                    it[Id] = 100500
                    it[Value] = "200700"
                }
                it[OtherOwnField] = 16_000_000_000
            })

            assertSame(rec[WithNested.OwnField], rec[WithNested.OwnField])
            // read uncommitted
            assertEquals("qwe", rec[WithNested.OwnField])
            assertEquals(100500, rec[WithNested.Nested][SchWithId.Id])
            assertEquals("200700", rec[WithNested.Nested][SchWithId.Value])
            assertEquals(16_000_000_000, rec[WithNested.OtherOwnField])

            rec
        }

        assertSame(rec[WithNested.OwnField], rec[WithNested.OwnField])
        assertEquals("qwe", rec[WithNested.OwnField])
        assertEquals(100500, rec[WithNested.Nested][SchWithId.Id])
        assertEquals("200700", rec[WithNested.Nested][SchWithId.Value])
        assertEquals("", rec[WithNested.Nested][SchWithId.MutValue])
        assertEquals(16_000_000_000, rec[WithNested.OtherOwnField])

        val emb = rec[WithNested.Nested]
        session.withTransaction {
            rec[WithNested.Nested] = SchWithId.build {
                it[Id] = 100500
                it[Value] = "200700"
                it[MutValue] = "mutated"
            }

            assertEquals("uncommitted should be visible through mut col", "mutated", emb[SchWithId.MutValue])
            // which is not guaranteed for immutable columns, though
        }

        val newEmb = rec[WithNested.Nested]
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
                .map(WithNested.Nested.ofStruct())
                .map(SchWithId.MutValue.ofStruct())
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

    @Test fun `embed nullable`() {
        val record = session.withTransaction {
            val rec = insert(TableWithNullableEmbed, WithNullableNested.build {
                it[OwnField] = "qwe"
                it[Nested] = null
                it[OtherOwnField] = 16_000_000_000
            })

            // read uncommitted
            assertEquals("qwe", rec[WithNullableNested.OwnField])
            assertEquals(null, rec[WithNullableNested.Nested])
            assertEquals(16_000_000_000, rec[WithNullableNested.OtherOwnField])

            rec
        }

        assertEquals("qwe", record[WithNullableNested.OwnField])
        assertEquals(null, record[WithNullableNested.Nested])
        assertEquals(16_000_000_000, record[WithNullableNested.OtherOwnField])

        session.withTransaction {
            record[WithNullableNested.Nested] = SchWithId.build {
                it[Id] = 100500
                it[Value] = "200700"
                it[MutValue] = "mutated"
            }
        }

        val emb = record[WithNullableNested.Nested]!!
        assertSame(emb, record[WithNullableNested.Nested])
        assertEquals(100500, emb[SchWithId.Id])
        assertEquals("200700", emb[SchWithId.Value])
        assertEquals("mutated", emb[SchWithId.MutValue])

        session.withTransaction {
            record[WithNullableNested.Nested] = null
        }

        assertEquals("qwe", record[WithNullableNested.OwnField])
        assertEquals(null, record[WithNullableNested.Nested])
        assertEquals(16_000_000_000, record[WithNullableNested.OtherOwnField])
    }

    @Test fun `embed partial`() {
        val rec = session.withTransaction {
            val rec = insert(TableWithPartialEmbed, WithPartialNested.build {
                it[Nested] = SchWithId.buildPartial {
                    it[Value] = "I'm another String!"
                }
                it[OwnField] = "I'm a String!"
            })

            assertSame(rec[WithPartialNested.Nested], rec[WithPartialNested.Nested])
            assertEquals(SchWithId.Value.asFieldSet(), rec[WithPartialNested.Nested].fields)
            assertEquals(-1L, rec[WithPartialNested.Nested].getOrDefault(SchWithId.Id, -1L))
            assertEquals("I'm another String!", rec[WithPartialNested.Nested].getOrDefault(SchWithId.Value, ""))
            assertEquals("!!!", rec[WithPartialNested.Nested].getOrDefault(SchWithId.MutValue, "!!!"))
            assertEquals("I'm a String!", rec[WithPartialNested.OwnField])

            rec
        }
        assertSame(rec[WithPartialNested.Nested], rec[WithPartialNested.Nested])
        assertEquals(SchWithId.Value.asFieldSet(), rec[WithPartialNested.Nested].fields)
        assertEquals(-1L, rec[WithPartialNested.Nested].getOrDefault(SchWithId.Id, -1L))
        assertEquals("I'm another String!", rec[WithPartialNested.Nested].getOrDefault(SchWithId.Value, ""))
        assertEquals("!!!", rec[WithPartialNested.Nested].getOrDefault(SchWithId.MutValue, "!!!"))
        assertEquals("I'm a String!", rec[WithPartialNested.OwnField])
        val nest = rec[WithPartialNested.Nested]

        session.withTransaction {
            rec[WithPartialNested.Nested] = rec[WithPartialNested.Nested].copy {
                it[MutValue] = "some real strings here!"
            }
        }
        assertNotSame(nest, rec[WithPartialNested.Nested])
        assertEquals("some real strings here!", rec[WithPartialNested.Nested].getOrThrow(SchWithId.MutValue))
    }

    /*@Test fun `embed nullable partial`() {
        TODO()
    }*/

}
