package net.aquadc.properties

import net.aquadc.properties.internal.compact
import net.aquadc.properties.internal.copyOfWithout
import net.aquadc.properties.internal.with
import net.aquadc.properties.internal.withoutNulls
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class ArrayUtilsTest {

    @Test fun with() {
        assertArrayEquals(arrayOf(1), emptyArray<Int>().with(1))
        assertArrayEquals(arrayOf(1, 2), arrayOf(1).with(2))
    }

    @Test fun copyWithout() {
        assertArrayEquals(arrayOf(1, 2), arrayOf(0, 1, 2).copyOfWithout(0, emptyArray()))
        assertArrayEquals(arrayOf(0, 2), arrayOf(0, 1, 2).copyOfWithout(1, emptyArray()))
        assertArrayEquals(arrayOf(0, 1), arrayOf(0, 1, 2).copyOfWithout(2, emptyArray()))
    }

    @Test fun withoutNulls() {
        assertArrayEquals(emptyArray(), arrayOf<Any?>(null).withoutNulls(emptyArray()))
        assertArrayEquals(emptyArray(), arrayOf<Any?>(null, null).withoutNulls(emptyArray()))
        assertArrayEquals(arrayOf(0, 1, 2), arrayOf<Any?>(0, null, 1, null, 2).withoutNulls(emptyArray()))
        assertArrayEquals(arrayOf(0, 2), arrayOf<Any?>(0, null, null, null, 2).withoutNulls(emptyArray()))
        assertArrayEquals(arrayOf(0, 1), arrayOf<Any?>(0, 1, null).withoutNulls(emptyArray()))
        assertArrayEquals(arrayOf(0, 1), arrayOf<Any?>(0, null, null, 1, null).withoutNulls(emptyArray()))
        assertArrayEquals(arrayOf(3, 4), arrayOf<Any?>(null, 3, 4).withoutNulls(emptyArray()))
        assertArrayEquals(arrayOf(3, 4), arrayOf<Any?>(null, null, 3, null, 4).withoutNulls(emptyArray()))
    }

    @Test fun compact() {
        assertEquals(-1, emptyArray<Any?>().compact())

        arrayOf<Any?>(null).assertCompact(arrayOf<Any?>(null), 0)
        arrayOf("__").assertCompact(arrayOf("__"), -1)

        arrayOf<Any?>(null, null).assertCompact(arrayOf<Any?>(null, null), 0)
        arrayOf("__", null).assertCompact(arrayOf("__", null), 1)
        arrayOf(null, "__").assertCompact(arrayOf("__", null), 1)
        arrayOf("__", "__").assertCompact(arrayOf("__", "__"), -1)

        arrayOf<Any?>(null, null, null).assertCompact(arrayOf<Any?>(null, null, null), 0)
        arrayOf(null, null, "__").assertCompact(arrayOf("__", null, null), 1)
        arrayOf(null, "__", null).assertCompact(arrayOf("__", null, null), 1)
        arrayOf(null, "__", "__").assertCompact(arrayOf("__", "__", null), 2)
        arrayOf("__", null, null).assertCompact(arrayOf("__", null, null), 1)
        arrayOf("__", null, "__").assertCompact(arrayOf("__", "__", null), 2)
        arrayOf("__", "__", null).assertCompact(arrayOf("__", "__", null), 2)
        arrayOf("__", "__", "__").assertCompact(arrayOf("__", "__", "__"), -1)

        arrayOf<Any?>(null, null, null, null).assertCompact(arrayOf<Any?>(null, null, null, null), 0)
        arrayOf(null, null, null, "__").assertCompact(arrayOf("__", null, null, null), 1)
        arrayOf(null, null, "__", null).assertCompact(arrayOf("__", null, null, null), 1)
        arrayOf(null, null, "__", "__").assertCompact(arrayOf("__", "__", null, null), 2)
        arrayOf(null, "__", null, null).assertCompact(arrayOf("__", null, null, null), 1)
        arrayOf(null, "__", null, "__").assertCompact(arrayOf("__", "__", null, null), 2)
        arrayOf(null, "__", "__", null).assertCompact(arrayOf("__", "__", null, null), 2)
        arrayOf(null, "__", "__", "__").assertCompact(arrayOf("__", "__", "__", null), 3)
        arrayOf("__", null, null, null).assertCompact(arrayOf("__", null, null, null), 1)
        arrayOf("__", null, null, "__").assertCompact(arrayOf("__", "__", null, null), 2)
        arrayOf("__", null, "__", null).assertCompact(arrayOf("__", "__", null, null), 2)
        arrayOf("__", null, "__", "__").assertCompact(arrayOf("__", "__", "__", null), 3)
        arrayOf("__", "__", null, null).assertCompact(arrayOf("__", "__", null, null), 2)
        arrayOf("__", "__", null, "__").assertCompact(arrayOf("__", "__", "__", null), 3)
        arrayOf("__", "__", "__", null).assertCompact(arrayOf("__", "__", "__", null), 3)
        arrayOf("__", "__", "__", "__").assertCompact(arrayOf("__", "__", "__", "__"), -1)
    }

    private fun Array<out Any?>.assertCompact(expectArray: Array<out Any?>, expectIdx: Int) {
        assertEquals(expectIdx, compact())
        assertArrayEquals(expectArray, this)
    }

}
