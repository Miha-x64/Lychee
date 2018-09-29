package net.aquadc.properties

import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test


class CommonTest {

    private val ump = propertyOf("")
    private val controlledUProps = listOf(
            ump,
            ump.map { "text: $it" },
            ump.mapWith(propertyOf("")) { a, b -> a + b },
            listOf(ump, propertyOf(""), propertyOf(""))
                    .mapValueList { vals -> vals.joinToString() }
    )

    private val cmp = concurrentPropertyOf("")
    private val controlledCProps = listOf(
            cmp,
            cmp.map { "text: $it" },
            cmp.mapWith(propertyOf("")) { a, b -> a + b },
            listOf(cmp, propertyOf(""), propertyOf(""))
                    .mapValueList { vals -> vals.joinToString() }
    )

    private fun testUnsubscription(controller: MutableProperty<String>, controlled: Property<String>) {
        var called = 0
        val onChange1 = { _: Any?, _: Any? -> called++; Unit }
        controlled.addUnconfinedChangeListener(onChange1)
        controller.value = "change"
        Assert.assertEquals(1, called)

        val onChange2 = { _: Any?, _: Any? -> called++; Unit }
        controlled.addUnconfinedChangeListener(onChange2)
        controller.value = "cone more time"
        Assert.assertEquals(3, called)

        val onChange3 = object : (Any?, Any?) -> Unit {
            override fun invoke(p1: Any?, p2: Any?) {
                controlled.removeChangeListener(this)
            }
        }
        controlled.addUnconfinedChangeListener(onChange3)
        controller.value = "he-he"
        Assert.assertEquals(5, called)
    }

    // no matter which type of property we have, should be called
    private fun testChange(controller: MutableProperty<String>, controlled: Property<String>) {
        var called = 0
        controlled.addUnconfinedChangeListener { _, _ -> called++; Unit }
        controller.value = controller.value
        assertEquals(1, called)
    }

    private fun testEquals(original: MutableProperty<String>) {
        val dist = original.distinct(areEqual())
        assertEquals(original.value, original.value)
        var called = 0
        dist.addUnconfinedChangeListener { _, _ -> called++; Unit }
        original.value = original.value
        assertEquals(0, called)
        assertEquals(original.value, original.value)

        original.value = " " + original.value
        assertEquals(1, called)
        assertEquals(original.value, original.value)
    }

    @Test fun nonSyncEquals() = testEquals(ump)
    @Test fun concEquals() = testEquals(cmp)

    private fun testAll(func: (MutableProperty<String>, Property<String>) -> Unit) {
        controlledUProps.forEach { func(ump, it) }
        controlledCProps.forEach { func(cmp, it) }
    }

    @Test fun unsubscription() = testAll(::testUnsubscription)
    @Test fun change() = testAll(::testChange)

    // different props have different identity, so we are going to test only mutable and mapped
    private fun testIdentity(original: MutableProperty<String>) {
        val dist = original.distinct(areIdentical())
        var called = 0
        dist.addUnconfinedChangeListener { _, _ -> called++; Unit }
        original.value = original.value
        assertEquals(0, called)
        assertEquals(original.value, dist.value)

        original.value = (" " + original.value).substring(1)
        assertEquals(1, called)
        assertEquals(original.value, dist.value)
    }

    @Test fun nonSyncIdentity() = testIdentity(ump)
    @Test fun concIdentity() = testIdentity(cmp)

}
