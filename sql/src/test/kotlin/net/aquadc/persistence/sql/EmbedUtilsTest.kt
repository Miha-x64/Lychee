package net.aquadc.persistence.sql

import net.aquadc.persistence.extended.build
import net.aquadc.persistence.extended.buildPartial
import net.aquadc.persistence.extended.either.Either
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.struct.build
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class EmbedUtilsTest {

    @Test fun someSchema() = assertWorks(
            SomeTable,
            SomeSchema.build {
                it[A] = "123"
                it[B] = 456
                it[C] = 789L
            },
            arrayOf("123", 456, 789L)
    )

    @Test fun withNested() = assertWorks(
            TableWithEmbed,
            WithNested.build {
                it[OwnField] = "123"
                it[Nested] = SchWithId.build {
                    it[Id] = 456L
                    it[Value] = "789"
                    it[MutValue] = "ABC"
                }
                it[OtherOwnField] = 0xDEFL
            },
            arrayOf("123", 456L, "789", "ABC", 0xDEFL)
    )

    @Test fun goDeeper() = assertWorks(
            WeNeedToGoDeeper,
            goDeeper.build(
                    DeeplyNested.build {
                        it[OwnField] = "01"
                        it[Nested] = WithNested.build {
                            it[OwnField] = "23"
                            it[Nested] = SchWithId.build {
                                it[Id] = 45L
                                it[Value] = "67"
                                it[MutValue] = "89"
                            }
                            it[OtherOwnField] = 0xABL
                        }
                    },
                    WithNullableNested.build {
                        it[OwnField] = "EF"
                        it[Nested] = SchWithId.build {
                            it[Id] = "GH".toLong(36)
                            it[Value] = "IJ"
                            it[MutValue] = "KL"
                        }
                        it[OtherOwnField] = "MN".toLong(36)
                    },
                    Either.Second(SomeSchema.build {
                        it[A] = "OP"
                        it[B] = "QR".toInt(36)
                        it[C] = "ST".toLong(36)
                    })
            ),
            arrayOf(
                    "01", "23", 45L, "67", "89", 0xABL,
                    "EF",
                    7L, // fieldSet
                    "GH".toLong(36), "IJ", "KL", "MN".toLong(36),
                    2L, // fieldSet
                    null, // First
                    7L, // fieldSet
                    "OP", "QR".toInt(36), "ST".toLong(36) // Second
            )
    )

    @Test fun nullableNested0() = assertWorks(
            TableWithNullableEmbed,
            WithNullableNested.build {
                it[OwnField] = "wat"
                it[Nested] = null
                it[OtherOwnField] = 123L
            },
            arrayOf(
                    "wat",
                    null, null, null, null, // fieldSet, 3 fields
                    123L
            )
    )

    @Test fun nullableNested1() = assertWorks(
            TableWithNullableEmbed,
            WithNullableNested.build {
                it[OwnField] = "wat"
                it[Nested] = SchWithId.build {
                    it[Id] = 123L
                    it[Value] = "456"
                    it[MutValue] = "789"
                }
                it[OtherOwnField] = 0xABCL
            },
            arrayOf(
                    "wat",
                    7L, 123L, "456", "789", // fieldSet, 3 fields
                    0xABCL
            )
    )

    @Test fun partialNested0() = assertWorks(
            TableWithPartialEmbed,
            WithPartialNested.build {
                it[Nested] = SchWithId.buildPartial {  }
                it[OwnField] = "zzz"
            },
            arrayOf(
                    0L, null, null, null, // fieldSet, 3 fields
                    "zzz"
            )
    )

    @Test fun partialNestedId() = assertWorks(
            TableWithPartialEmbed,
            WithPartialNested.build {
                it[Nested] = SchWithId.buildPartial {
                    it[Id] = 123L
                }
                it[OwnField] = "zzz"
            },
            arrayOf(
                    1L, 123L, null, null, // fieldSet, 3 fields
                    "zzz"
            )
    )

    @Test fun partialNestedValue() = assertWorks(
            TableWithPartialEmbed,
            WithPartialNested.build {
                it[Nested] = SchWithId.buildPartial {
                    it[Value] = "456"
                }
                it[OwnField] = "zzz"
            },
            arrayOf(
                    2L, null, "456", null, // fieldSet, 3 fields
                    "zzz"
            )
    )

    @Test fun partialNestedMutValue() = assertWorks(
            TableWithPartialEmbed,
            WithPartialNested.build {
                it[Nested] = SchWithId.buildPartial {
                    it[MutValue] = "789"
                }
                it[OwnField] = "zzz"
            },
            arrayOf(
                    4L, null, null, "789", // fieldSet, 3 fields
                    "zzz"
            )
    )

    @Test fun partialNestedIdValue() = assertWorks(
            TableWithPartialEmbed,
            WithPartialNested.build {
                it[Nested] = SchWithId.buildPartial {
                    it[Id] = 111L
                    it[Value] = "222"
                }
                it[OwnField] = "xxx"
            },
            arrayOf(
                    3L, 111L, "222", null, // fieldSet, 3 fields
                    "xxx"
            )
    )

    @Test fun partialNestedIdMutValue() = assertWorks(
            TableWithPartialEmbed,
            WithPartialNested.build {
                it[Nested] = SchWithId.buildPartial {
                    it[Id] = 111L
                    it[MutValue] = "222"
                }
                it[OwnField] = "xxx"
            },
            arrayOf(
                    5L, 111L, null, "222", // fieldSet, 3 fields
                    "xxx"
            )
    )

    @Test fun partialNestedValueMutValue() = assertWorks(
            TableWithPartialEmbed,
            WithPartialNested.build {
                it[Nested] = SchWithId.buildPartial {
                    it[Value] = "111"
                    it[MutValue] = "222"
                }
                it[OwnField] = "xxx"
            },
            arrayOf(
                    6L, null, "111", "222", // fieldSet, 3 fields
                    "xxx"
            )
    )

    @Test fun partialNestedAll() = assertWorks(
            TableWithPartialEmbed,
            WithPartialNested.build {
                it[Nested] = SchWithId.buildPartial {
                    it[Id] = 111L
                    it[Value] = "222"
                    it[MutValue] = "333"
                }
                it[OwnField] = "xxx"
            },
            arrayOf(
                    7L, 111L, "222", "333", // fieldSet, 3 fields
                    "xxx"
            )
    )

    private fun <SCH : Schema<SCH>> assertWorks(table: SimpleTable<SCH, Long>, value: Struct<SCH>, flatExpect: Array<out Any?>) {
        val dest = arrayOfNulls<Any>(table.columnsMappedToFields.size)
        flatten(table.recipe, dest, value, 0, 0)
        assertArrayEquals(flatExpect, dest)

        inflate(table.recipe, dest, 0, 0, 0)
        assertEquals(value, dest[0])
    }

}
