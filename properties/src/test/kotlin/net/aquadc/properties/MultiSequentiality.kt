package net.aquadc.properties

import org.junit.Assert.assertEquals
import org.junit.Test


class MultiSequentiality {

    @Test fun `bi-mapped on single thread`() {
        val q = propertyOf(0)
        val w = propertyOf(0)
        singleThreaded(q, w, q.mapWith(w) { a, s -> a + s })
    }
    @Test fun `concurrent bi-mapped on single thread`() {
        val q = concurrentPropertyOf(0)
        val w = concurrentPropertyOf(0)
        singleThreaded(q, w, q.mapWith(w) { a, s -> a + s })
    }
    @Test fun `multi-mapped on single thread`() {
        val q = propertyOf(0)
        val w = propertyOf(0)
        singleThreaded(q, w, listOf(q, w).mapValueList { (a, s) -> a + s })
    }
    @Test fun `concurrent multi-mapped on single thread`() {
        val q = concurrentPropertyOf(0)
        val w = concurrentPropertyOf(0)
        singleThreaded(q, w, listOf(q, w).mapValueList { (a, s) -> a + s })
    }
    private fun singleThreaded(driver1: MutableProperty<Int>, driver2: MutableProperty<Int>, sum: Property<Int>) {
        val valueList = arrayListOf<Int>()
        sum.addUnconfinedChangeListener { _, new ->
            if (new == 0) {
                driver1.value = 1
                driver2.value = 2
            }
            valueList.add(new)
        }
        driver1.value = 0

        // 1. d1 = 0; d2 = 0
        // -- hung in notification
        // 2. d1 = 1 postponed
        // 3. d2 = 2; d1 remembered as 0
        // 4. d1 unhangs, 1; 2

        assertEquals(listOf(0, 2, 3), valueList)
    }

}
