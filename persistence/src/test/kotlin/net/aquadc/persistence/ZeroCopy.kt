package net.aquadc.persistence

import net.aquadc.persistence.type.collection
import net.aquadc.persistence.type.i64
import net.aquadc.persistence.type.set
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test


class ZeroCopy {

    @Test fun encode() {
        val c = collection(i64)
        val s = set(i64)

        val longCollection = setOf(0L)
        assertSame(longCollection, s.store(longCollection))

        val longList = listOf(0L)
        assertSame(longList, c.store(longList))

        val longBoxArray = arrayOf(0L)
        val longBoxArrayBox = longBoxArray.asList()
        assertSame(longBoxArrayBox, c.load(longBoxArrayBox))

        val longArray = longArrayOf(0L)
        val longArrayBox = longArray.asList()
        assertSame(longArrayBox, c.load(longArrayBox))
    }

    @Test fun decodeLongCollect() {
        val t = collection(i64)

        val longCollection = setOf(0L)
        assertNotSame(longCollection, t.load(longCollection)) // Set cannot be List

        val longList = listOf(0L)
        assertSame(longList, t.load(longList))

        val longBoxArray = arrayOf(0L)
        assertNotSame(longBoxArray, t.load(longBoxArray))

        val longBoxArrayBox = longBoxArray.asList()
        assertSame(longBoxArrayBox, t.load(longBoxArrayBox))

        val longArray = longArrayOf(0L)
        assertNotSame(longArray, t.load(longArray))

        val longArrayBox = longArray.asList()
        assertSame(longArrayBox, t.load(longArrayBox))
    }

    @Test fun decodeLongSet() {
        val t = set(i64)

        val longCollection = setOf(0L)
        assertSame(longCollection, t.load(longCollection))

        val longList = listOf(0L)
        assertNotSame(longList, t.load(longList))

        val longBoxArray = arrayOf(0L)
        assertNotSame(longBoxArray, t.load(longBoxArray))

        val longBoxArrayBox = longBoxArray.asList()
        assertNotSame(longBoxArrayBox, t.load(longBoxArrayBox))

        val longArray = longArrayOf(0L)
        assertNotSame(longArray, t.load(longArray))

        val longArrayBox = longArray.asList()
        assertNotSame(longArrayBox, t.load(longArrayBox))
    }

}
