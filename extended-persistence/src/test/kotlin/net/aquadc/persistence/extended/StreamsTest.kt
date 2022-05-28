package net.aquadc.persistence.extended

import net.aquadc.persistence.extended.either.EitherFourth
import net.aquadc.persistence.extended.either.EitherLeft
import net.aquadc.persistence.extended.either.EitherRight
import net.aquadc.persistence.extended.either.either
import net.aquadc.persistence.extended.either.either4
import net.aquadc.persistence.extended.tuple.Tuple4
import net.aquadc.persistence.extended.tuple.invoke
import net.aquadc.persistence.stream.DataStreams
import net.aquadc.persistence.stream.read
import net.aquadc.persistence.stream.write
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.nullable
import net.aquadc.persistence.type.string
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
                SomeSchema.Partial {  },
                read(partialSchema, write(partialSchema, SomeSchema.Partial {  }))
        )
    }

    @Test fun `single struct from&to stream`() {
        assertEquals(
                SomeSchema.Partial { it[A] = "some" },
                read(partialSchema, write(partialSchema, SomeSchema.Partial { it[A] = "some" }))
        )
    }

    @Test fun `full struct from&to stream`() {
        val partial = SomeSchema.Partial { it[A] = "lorem"; it[B] = 11; it[C] = 31L }
        assertEquals(
                partial,
                read(partialSchema, write(partialSchema, partial))
        )
    }

    private val userType = Tuple4(
            "name", string,
            "surname", string,
            "", either(
                "second_name", string,
                "patronymic", string
            ),
            "transport", either4(
                    "preferred_public_transport", string,
                    "bike_serial_number", string,
                    "car_number", string,
                    "horse_name", string
            )
    )
    @Test fun either() {
        val i = userType(
                "Ivan", "Ivanov", EitherRight("Ivanovich"), EitherRight("100500")
        )
        assertEquals(i, read(userType, write(userType, i)))

        val j = userType(
                "Jake", "Wharton", EitherLeft("???"), EitherFourth("Andrew")
        )
        assertEquals(j, read(userType, write(userType, j)))
    }

    private fun <T> read(type: DataType<T>, value: ByteArray): T =
            DataStreams.read(DataInputStream(ByteArrayInputStream(value)), type)

    private fun <T> write(type: DataType<T>, value: T): ByteArray =
            ByteArrayOutputStream().also { DataStreams.write(DataOutputStream(it), type, value) }.toByteArray()

}
