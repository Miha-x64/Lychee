package net.aquadc.properties

import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test


class CommonTest {

    private val ump = unsynchronizedMutablePropertyOf("")
    private val controlledUProps = listOf(
            ump,
            ump.map { "text: $it" },
            ump.mapWith(concurrentMutablePropertyOf("")) { a, b -> a + b },
            listOf(ump, concurrentMutablePropertyOf(""), concurrentMutablePropertyOf("")).mapValueList { vals -> vals.joinToString() }
    )

    private val cmp = concurrentMutablePropertyOf("")
    private val controlledCProps = listOf(
            cmp,
            cmp.map { "text: $it" },
            cmp.mapWith(unsynchronizedMutablePropertyOf("")) { a, b -> a + b },
            listOf(cmp, unsynchronizedMutablePropertyOf(""), unsynchronizedMutablePropertyOf(""))
                    .mapValueList { vals -> vals.joinToString() }
    )

    private fun testUnsubscription(controller: MutableProperty<String>, controlled: Property<String>) {
        var called = 0
        val onChange1 = { _: Any?, _: Any? -> called++; Unit }
        controlled.addChangeListener(onChange1)
        controller.value = "change"
        Assert.assertEquals(1, called)

        val onChange2 = { _: Any?, _: Any? -> called++; Unit }
        controlled.addChangeListener(onChange2)
        controller.value = "cone more time"
        Assert.assertEquals(3, called)

        val onChange3 = object : (Any?, Any?) -> Unit {
            override fun invoke(p1: Any?, p2: Any?) {
                controlled.removeChangeListener(this)
            }
        }
        controlled.addChangeListener(onChange3)
        controller.value = "he-he"
        Assert.assertEquals(5, called)
    }

    // no matter which type of property we have, should be called
    private fun testChange(controller: MutableProperty<String>, controlled: Property<String>) {
        var called = 0
        controlled.addChangeListener { _, _ -> called++; Unit }
        controller.value = controller.value
        assertEquals(1, called)
    }

    private fun testEqualsDistinctChange(controller: MutableProperty<String>, controlled: Property<String>) {
        val prop = controlled.distinct(Equals)
        var called = 0
        prop.addChangeListener { _, _ -> called++; Unit }
        controller.value = controller.value
        assertEquals(0, called)

        controller.value = " " + controller.value
        assertEquals(1, called)
    }

    private fun testAll(func: (MutableProperty<String>, Property<String>) -> Unit) {
        controlledUProps.forEach { func(ump, it) }
        controlledCProps.forEach { func(cmp, it) }
    }

    @Test fun unsubscription() = testAll(::testUnsubscription)
    @Test fun change() = testAll(::testChange)
    @Test fun equalsChange() = testAll(::testEqualsDistinctChange)

    // different props have different identity, so we are going to test only mutable and mapped
    fun testIdentity(controller: MutableProperty<String>, controlled: Property<String>) {
        val prop = controlled.distinct(Identity)
        var called = 0
        prop.addChangeListener { _, _ -> called++; Unit }
        controller.value = controller.value
        assertEquals(0, called)

        controller.value = (" " + controller.value).substring(1)
        assertEquals(1, called)
    }

    @Test fun ideitity() {
        testIdentity(ump, ump)
        testIdentity(cmp, cmp)
    }

}
