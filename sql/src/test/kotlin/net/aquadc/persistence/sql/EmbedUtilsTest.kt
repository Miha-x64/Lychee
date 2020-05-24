package net.aquadc.persistence.sql

import net.aquadc.persistence.extended.buildPartial
import net.aquadc.persistence.extended.either.EitherRight
import net.aquadc.persistence.extended.invoke
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.struct.invoke
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class EmbedUtilsTest {

    @Test fun someSchema() = assertWorks(
            SomeTable,
            SomeSchema {
                it[A] = "123"
                it[B] = 456
                it[C] = 789L
            },
            arrayOf("123", 456, 789L)
    )

    @Test fun withNested() = assertWorks(
            TableWithEmbed,
            WithNested {
                it[OwnField] = "123"
                it[Nested] = SchWithId {
                    it[Id] = 456
                    it[Value] = "789"
                    it[MutValue] = "ABC"
                }
                it[OtherOwnField] = 0xDEFL
            },
            arrayOf("123", 456, "789", "ABC", 0xDEFL)
    )

    @Test fun goDeeper() = assertWorks(
            WeNeedToGoDeeper,
            goDeeper(
                    DeeplyNested {
                        it[OwnField] = "01"
                        it[Nested] = WithNested {
                            it[OwnField] = "23"
                            it[Nested] = SchWithId {
                                it[Id] = 45
                                it[Value] = "67"
                                it[MutValue] = "89"
                            }
                            it[OtherOwnField] = 0xABL
                        }
                    },
                    WithNullableNested {
                        it[OwnField] = "EF"
                        it[Nested] = SchWithId {
                            it[Id] = "GH".toInt(36)
                            it[Value] = "IJ"
                            it[MutValue] = "KL"
                        }
                        it[OtherOwnField] = "MN".toLong(36)
                    },
                    EitherRight(SomeSchema {
                        it[A] = "OP"
                        it[B] = "QR".toInt(36)
                        it[C] = "ST".toLong(36)
                    })
            ),
            arrayOf(
                    "01", "23", 45, "67", "89", 0xABL,
                    "EF",
                    7L, // fieldSet
                    "GH".toInt(36), "IJ", "KL", "MN".toLong(36),
                    2L, // fieldSet
                    null, // First
                    7L, // fieldSet
                    "OP", "QR".toInt(36), "ST".toLong(36) // Second
            )
    )

    @Test fun nullableNested0() = assertWorks(
            TableWithNullableEmbed,
            WithNullableNested {
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
            WithNullableNested {
                it[OwnField] = "wat"
                it[Nested] = SchWithId {
                    it[Id] = 123
                    it[Value] = "456"
                    it[MutValue] = "789"
                }
                it[OtherOwnField] = 0xABCL
            },
            arrayOf(
                    "wat",
                    7L, 123, "456", "789", // fieldSet, 3 fields
                    0xABCL
            )
    )

    @Test fun partialNested0() = assertWorks(
            TableWithPartialEmbed,
            WithPartialNested {
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
            WithPartialNested {
                it[Nested] = SchWithId.buildPartial {
                    it[Id] = 123
                }
                it[OwnField] = "zzz"
            },
            arrayOf(
                    1L, 123, null, null, // fieldSet, 3 fields
                    "zzz"
            )
    )

    @Test fun partialNestedValue() = assertWorks(
            TableWithPartialEmbed,
            WithPartialNested {
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
            WithPartialNested {
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
            WithPartialNested {
                it[Nested] = SchWithId.buildPartial {
                    it[Id] = 111
                    it[Value] = "222"
                }
                it[OwnField] = "xxx"
            },
            arrayOf(
                    3L, 111, "222", null, // fieldSet, 3 fields
                    "xxx"
            )
    )

    @Test fun partialNestedIdMutValue() = assertWorks(
            TableWithPartialEmbed,
            WithPartialNested {
                it[Nested] = SchWithId.buildPartial {
                    it[Id] = 111
                    it[MutValue] = "222"
                }
                it[OwnField] = "xxx"
            },
            arrayOf(
                    5L, 111, null, "222", // fieldSet, 3 fields
                    "xxx"
            )
    )

    @Test fun partialNestedValueMutValue() = assertWorks(
            TableWithPartialEmbed,
            WithPartialNested {
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
            WithPartialNested {
                it[Nested] = SchWithId.buildPartial {
                    it[Id] = 111
                    it[Value] = "222"
                    it[MutValue] = "333"
                }
                it[OwnField] = "xxx"
            },
            arrayOf(
                    7L, 111, "222", "333", // fieldSet, 3 fields
                    "xxx"
            )
    )

    private fun <SCH : Schema<SCH>> assertWorks(table: SimpleTable<SCH, *>, value: Struct<SCH>, flatExpect: Array<out Any?>) {
        val dest = arrayOfNulls<Any>(table.managedColNames.size)
        flatten(table.recipe, dest, value, 0, 0)
        assertArrayEquals(flatExpect, dest)

        inflate(table.recipe, dest, 0, 0, 0)
        assertEquals(value, dest[0])
    }

}
