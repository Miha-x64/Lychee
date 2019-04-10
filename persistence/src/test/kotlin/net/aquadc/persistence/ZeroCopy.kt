package net.aquadc.persistence

import net.aquadc.persistence.type.collection
import net.aquadc.persistence.type.long
import net.aquadc.persistence.type.set
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test


class ZeroCopy {

    @Test fun encode() {
        val c = collection(long)
        val s = set(long)

        val longCollection = setOf(0L)
        assertSame(longCollection, s.encode(longCollection))

        val longList = listOf(0L)
        assertSame(longList, c.encode(longList))

        val longBoxArray = arrayOf(0L)
        val longBoxArrayBox = longBoxArray.asList()
        assertSame(longBoxArrayBox, c.decodeCollection(longBoxArrayBox))

        val longArray = longArrayOf(0L)
        val longArrayBox = longArray.asList()
        assertSame(longArrayBox, c.decodeCollection(longArrayBox))
    }

    @Test fun decodeLongCollect() {
        val t = collection(long)

        val longCollection = setOf(0L)
        assertNotSame(longCollection, t.decodeCollection(longCollection)) // Set cannot be List

        val longList = listOf(0L)
        assertSame(longList, t.decodeCollection(longList))

        val longBoxArray = arrayOf(0L)
        assertNotSame(longBoxArray, t.decodeCollection(longBoxArray))

        val longBoxArrayBox = longBoxArray.asList()
        assertSame(longBoxArrayBox, t.decodeCollection(longBoxArrayBox))

        val longArray = longArrayOf(0L)
        assertNotSame(longArray, t.decodeCollection(longArray))

        val longArrayBox = longArray.asList()
        assertSame(longArrayBox, t.decodeCollection(longArrayBox))
    }

    @Test fun decodeLongSet() {
        val t = set(long)

        val longCollection = setOf(0L)
        assertSame(longCollection, t.decodeCollection(longCollection))

        val longList = listOf(0L)
        assertNotSame(longList, t.decodeCollection(longList))

        val longBoxArray = arrayOf(0L)
        assertNotSame(longBoxArray, t.decodeCollection(longBoxArray))

        val longBoxArrayBox = longBoxArray.asList()
        assertNotSame(longBoxArrayBox, t.decodeCollection(longBoxArrayBox))

        val longArray = longArrayOf(0L)
        assertNotSame(longArray, t.decodeCollection(longArray))

        val longArrayBox = longArray.asList()
        assertNotSame(longArrayBox, t.decodeCollection(longArrayBox))
    }

}
