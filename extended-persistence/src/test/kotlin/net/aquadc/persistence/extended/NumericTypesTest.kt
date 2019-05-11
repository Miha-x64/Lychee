package net.aquadc.persistence.extended

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class NumericTypesTest {

    @Test fun `byte array`() {
        assertArrayEquals(byteArrayOf(1, 2, 3), byteCollection.load(byteArrayOf(1, 2, 3)))
        assertArrayEquals(byteArrayOf(1, 2, 3), byteCollection.load(arrayOf(1.toByte(), 2.toByte(), 3.toByte())))
        assertArrayEquals(byteArrayOf(1, 2, 3), byteCollection.load(listOf(1.toByte(), 2.toByte(), 3.toByte())))
        assertArrayEquals(byteArrayOf(1, 2, 3), byteCollection.store(byteArrayOf(1, 2, 3)) as ByteArray)
    }

    @Test fun `short array`() {
        assertArrayEquals(shortArrayOf(1, 2, 3), shortCollection.load(shortArrayOf(1, 2, 3)))
        assertArrayEquals(shortArrayOf(1, 2, 3), shortCollection.load(arrayOf(1.toShort(), 2.toShort(), 3.toShort())))
        assertArrayEquals(shortArrayOf(1, 2, 3), shortCollection.load(listOf(1.toShort(), 2.toShort(), 3.toShort())))
        assertArrayEquals(shortArrayOf(1, 2, 3), shortCollection.store(shortArrayOf(1, 2, 3)) as ShortArray)
    }

    @Test fun `int array`() {
        assertArrayEquals(intArrayOf(1, 2, 3), intCollection.load(intArrayOf(1, 2, 3)))
        assertArrayEquals(intArrayOf(1, 2, 3), intCollection.load(arrayOf(1, 2, 3)))
        assertArrayEquals(intArrayOf(1, 2, 3), intCollection.load(listOf(1, 2, 3)))
        assertArrayEquals(intArrayOf(1, 2, 3), intCollection.store(intArrayOf(1, 2, 3)) as IntArray)
    }

    @Test fun `long array`() {
        assertArrayEquals(longArrayOf(1, 2, 3), longCollection.load(longArrayOf(1, 2, 3)))
        assertArrayEquals(longArrayOf(1, 2, 3), longCollection.load(arrayOf(1L, 2L, 3L)))
        assertArrayEquals(longArrayOf(1, 2, 3), longCollection.load(listOf(1L, 2L, 3L)))
        assertArrayEquals(longArrayOf(1, 2, 3), longCollection.store(longArrayOf(1, 2, 3)) as LongArray)
    }

    @Test fun `float array`() {
        assertArrayEquals(floatArrayOf(1f, 2f, 3f), floatCollection.load(floatArrayOf(1f, 2f, 3f)), 0f)
        assertArrayEquals(floatArrayOf(1f, 2f, 3f), floatCollection.load(arrayOf(1f, 2f, 3f)), 0f)
        assertArrayEquals(floatArrayOf(1f, 2f, 3f), floatCollection.load(listOf(1f, 2f, 3f)), 0f)
        assertArrayEquals(floatArrayOf(1f, 2f, 3f), floatCollection.store(floatArrayOf(1f, 2f, 3f)) as FloatArray, 0f)
    }

    @Test fun `double array`() {
        assertArrayEquals(doubleArrayOf(1.0, 2.0, 3.0), doubleCollection.load(doubleArrayOf(1.0, 2.0, 3.0)), 0.0)
        assertArrayEquals(doubleArrayOf(1.0, 2.0, 3.0), doubleCollection.load(arrayOf(1.0, 2.0, 3.0)), 0.0)
        assertArrayEquals(doubleArrayOf(1.0, 2.0, 3.0), doubleCollection.load(listOf(1.0, 2.0, 3.0)), 0.0)
        assertArrayEquals(doubleArrayOf(1.0, 2.0, 3.0), doubleCollection.store(doubleArrayOf(1.0, 2.0, 3.0)) as DoubleArray, 0.0)
    }

    @Test fun uByte() {
        assertEquals(10.toUByte(), uByte.load(10.toShort()))
        assertEquals(100.toUByte(), uByte.load(100.toShort()))
        assertEquals(200.toUByte(), uByte.load(200.toShort()))
        assertEquals(200.toShort(), uByte.store(200.toUByte()))
        assertEquals(0.toShort(), uByte.store(0.toUByte()))
    }

    @Test(expected = IllegalArgumentException::class) fun `incorrect uByte`() {
        uByte.load(256.toShort())
    }

    @Test fun uShort() {
        assertEquals(10.toUShort(), uShort.load(10))
        assertEquals(65535.toUShort(), uShort.load(65535))
        assertEquals(10000, uShort.store(10000.toUShort()))
    }

    @Test(expected = IllegalArgumentException::class) fun `incorrect uShort`() {
        uShort.load(65536)
    }

    @Test fun uInt() {
        assertEquals(10.toUInt(), uInt.load(10L))
        assertEquals(4_000_000_000L.toUInt(), uInt.load(4_000_000_000L))
        assertEquals(4_000_000_000L, uInt.store(4_000_000_000L.toUInt()))
    }

    @Test(expected = IllegalArgumentException::class) fun `incorrect uInt`() {
        uInt.load(5_000_000_000L)
    }

}
