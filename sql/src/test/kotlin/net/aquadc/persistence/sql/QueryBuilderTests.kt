package net.aquadc.persistence.sql

import net.aquadc.persistence.extended.build
import net.aquadc.persistence.extended.either.EitherLeft
import net.aquadc.persistence.extended.either.EitherRight
import net.aquadc.persistence.struct.build
import org.junit.Assert.assertEquals
import org.junit.Test


open class QueryBuilderTests {

    open val session: Session<*> get() = jdbcSession

    @Test fun between() {
        session.withTransaction {
            repeat(10) { i ->
                insert(SomeTable, SomeSchema.build {
                    it[A] = i.toString()
                    it[B] = i
                    it[C] = i.toLong()
                })
            }
        }

        try {
            val list = session[SomeTable].select(SomeSchema.B between 3..5).value
            assertEquals(listOf("3", "4", "5"), list.map(SomeSchema.A))
            assertEquals(listOf(3, 4, 5), list.map(SomeSchema.B))
            assertEquals(listOf(3L, 4L, 5L), list.map(SomeSchema.C))
        } finally {
            session.withTransaction {
                truncate(SomeTable)
            }
        }
    }

    @Test fun nested() {
        session.withTransaction {
            repeat(10) { i ->
                insert(TableWithEmbed, WithNested.build {
                    it[OwnField] = i.toString()
                    it[Nested] = SchWithId.build {
                        it[Id] = i.toInt()
                        it[Value] = i.toString()
                        it[MutValue] = i.toString()
                    }
                    it[OtherOwnField] = i.toLong()
                })
            }
        }

        try {
            val list = session[TableWithEmbed]
                    .select(WithNested.Nested / SchWithId.Value isIn arrayOf("1", "3", "5", "7")).value
            assertEquals(listOf("1", "3", "5", "7"), list.map(WithNested.OwnField))
            assertEquals(listOf(1, 3, 5, 7), list.map(WithNested.Nested / SchWithId.Id))
            assertEquals(listOf("1", "3", "5", "7"), list.map(WithNested.Nested / SchWithId.Value))
            assertEquals(listOf("1", "3", "5", "7"), list.map(WithNested.Nested / SchWithId.MutValue))
            assertEquals(listOf(1L, 3L, 5L, 7L), list.map(WithNested.OtherOwnField))
        } finally {
            session.withTransaction {
                truncate(SomeTable)
            }
        }
    }

    @Test fun like() {
        session.withTransaction {
            insert(SomeTable, SomeSchema.build {
                it[A] = "hello"
                it[B] = 0
                it[C] = 0L
            })
            insert(SomeTable, SomeSchema.build {
                it[A] = "goodbye"
                it[B] = 0
                it[C] = 0L
            })
        }

        val dao = session[SomeTable]
        try {
            assertEquals(listOf("hello"), dao.select(SomeSchema.A like "%hell%").value.map(SomeSchema.A))
            assertEquals(listOf("hello"), dao.select(SomeSchema.A contains "hell").value.map(SomeSchema.A))
            assertEquals(listOf("hello"), dao.select(SomeSchema.A startsWith "hell").value.map(SomeSchema.A))
            assertEquals(listOf("goodbye"), dao.select(SomeSchema.A notLike "%hell%").value.map(SomeSchema.A))
            assertEquals(listOf("hello"), dao.select(SomeSchema.A startsWith "h").value.map(SomeSchema.A))
            assertEquals(listOf("hello", "goodbye"), dao.select(SomeSchema.A contains "o").value.map(SomeSchema.A))
            assertEquals(listOf("hello"), dao.select(SomeSchema.A endsWith "o").value.map(SomeSchema.A))
        } finally {
            session.withTransaction {
                truncate(SomeTable)
            }
        }
    }

    @Test fun either() {
        session.withTransaction {
            insert(TableWithNullableEither, schemaWithNullableEither.build(EitherLeft("left"), null))
            insert(TableWithNullableEither, schemaWithNullableEither.build(EitherRight("right"), null))
            insert(TableWithNullableEither, schemaWithNullableEither.build(EitherLeft("left"), EitherLeft("left")))
            insert(TableWithNullableEither, schemaWithNullableEither.build(EitherLeft("left"), EitherRight("right")))
            insert(TableWithNullableEither, schemaWithNullableEither.build(EitherRight("right"), EitherLeft("left")))
            insert(TableWithNullableEither, schemaWithNullableEither.build(EitherRight("right"), EitherRight("right")))
        }

        val dao = session[TableWithNullableEither]
        assertEquals(
                listOf(1L, 3L, 4L),
                dao.select(schemaWithNullableEither.First % stringOrNullableString.schema.First eq "left").value.map(Record<*, *>::primaryKey)
        )
        assertEquals(
                listOf(2L, 5L, 6L),
                dao.select(schemaWithNullableEither.First % stringOrNullableString.schema.Second eq "right").value.map(Record<*, *>::primaryKey)
        )
        assertEquals(
                listOf(3L, 5L),
                dao.select(schemaWithNullableEither.Second % stringOrNullableString.schema.First eq "left").value.map(Record<*, *>::primaryKey)
        )
        assertEquals(
                listOf(4L, 6L),
                dao.select(schemaWithNullableEither.Second % stringOrNullableString.schema.Second eq "right").value.map(Record<*, *>::primaryKey)
        )
    }

}
