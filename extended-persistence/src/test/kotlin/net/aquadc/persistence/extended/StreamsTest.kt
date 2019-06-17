package net.aquadc.persistence.extended

import net.aquadc.persistence.stream.DataStreams
import net.aquadc.persistence.stream.read
import net.aquadc.persistence.stream.write
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.nullable
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream


class StreamsTest {

    private val nullableSchema = nullable(SomeSchema)
    private val partialSchema = partial(SomeSchema)

    @Test fun `null struct from&to stream`() {
        assertEquals(
                null,
                read(nullableSchema, write(nullableSchema, null))
        )
    }

    @Test fun `empty struct from&to stream`() {
        assertEquals(
                SomeSchema.buildPartial {  },
                read(partialSchema, write(partialSchema, SomeSchema.buildPartial {  }))
        )
    }

    @Test fun `single struct from&to stream`() {
        assertEquals(
                SomeSchema.buildPartial { it[A] = "some" },
                read(partialSchema, write(partialSchema, SomeSchema.buildPartial { it[A] = "some" }))
        )
    }

    @Test fun `full struct from&to stream`() {
        val partial = SomeSchema.buildPartial { it[A] = "lorem"; it[B] = 11; it[C] = 31L }
        assertEquals(
                partial,
                read(partialSchema, write(partialSchema, partial))
        )
    }

    private fun <T> read(type: DataType<T>, value: ByteArray): T =
            DataStreams.read(DataInputStream(ByteArrayInputStream(value)), type)

    private fun <T> write(type: DataType<T>, value: T): ByteArray =
            ByteArrayOutputStream().also { DataStreams.write(DataOutputStream(it), type, value) }.toByteArray()

}
