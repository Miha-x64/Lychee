package net.aquadc.properties

import org.junit.Assert.assertEquals
import org.junit.Test


class BindingTest {

    @Test fun concBinding() = binding(true)
    @Test fun unsBinding() = binding(false)
    private fun binding(concurrent: Boolean) {
        val mutable = mutablePropertyOf("hello", concurrent)
        val sample = mutablePropertyOf("world", concurrent)

        mutable.bindTo(sample)
        assertEquals("world", mutable.value)

        sample.value = "goodbye"
        assertEquals("goodbye", mutable.value)

        mutable.value = "hey"
        sample.value = "unbound"
        assertEquals("hey", mutable.value)

        var new: String? = null
        sample.value = "just bound"
        mutable.addChangeListener { _, n -> new = n }
        mutable.bindTo(sample)
        assertEquals("just bound", new)

        sample.value = "rebound"
        assertEquals("rebound", mutable.value)

        val newSample = mutablePropertyOf("another", concurrent)
        mutable.bindTo(newSample)
        assertEquals("another", mutable.value)

        sample.value = "bound to another"
        assertEquals("another", mutable.value)

        newSample.value = "bound to this"
        assertEquals("bound to this", mutable.value)
    }

    @Test fun concBindingToImmutable() = bindingToImmutable(true)
    @Test fun unsBindingToImmutable() = bindingToImmutable(false)
    /**
     * Same as previous one, but immutable property binding implementation differs
     */
    private fun bindingToImmutable(concurrent: Boolean) {
        val mutable = mutablePropertyOf("hello", concurrent)
        val immutable = immutablePropertyOf("world")

        mutable.bindTo(immutable)
        assertEquals("world", mutable.value)

        mutable.value = "hey"
        assertEquals("hey", mutable.value)
    }

    @Test fun testConcSub() {
        val p = concurrentMutablePropertyOf("")
        testUnsubscription(p, p)
    }
    @Test fun testConcMapSub() {
        val p = concurrentMutablePropertyOf("")
        testUnsubscription(p, p.map { "text: $it" })
    }
    @Test fun testConcBiMapSub() {
        val p = concurrentMutablePropertyOf("")
        testUnsubscription(p, p.mapWith(concurrentMutablePropertyOf("")) { a, b -> a + b })
    }
    @Test fun testConcMultiMapSub() {
        val p = concurrentMutablePropertyOf("")
        testUnsubscription(p,
                listOf(p, concurrentMutablePropertyOf(""), concurrentMutablePropertyOf(""))
                        .mapValueList { vals -> vals.joinToString() }
        )
    }
    @Test fun testUnsSub() {
        val p = unsynchronizedMutablePropertyOf("")
        testUnsubscription(p, p)
    }
    @Test fun testUnsMapSub() {
        val p = unsynchronizedMutablePropertyOf("")
        testUnsubscription(p, p.map { "text: $it" })
    }
    @Test fun testUnsBiMapSub() {
        val p = unsynchronizedMutablePropertyOf("")
        testUnsubscription(p, p.mapWith(unsynchronizedMutablePropertyOf("")) { a, b -> a + b })
    }
    @Test fun testUnsMultiMapSub() {
        val p = unsynchronizedMutablePropertyOf("")
        testUnsubscription(p,
                listOf(p, unsynchronizedMutablePropertyOf(""), unsynchronizedMutablePropertyOf(""))
                        .mapValueList { vals -> vals.joinToString()}
        )
    }

    private fun testUnsubscription(controller: MutableProperty<String>, controlled: Property<String>) {
        var called = 0
        val onChange1 = { _: Any?, _: Any? -> called++; Unit }
        controlled.addChangeListener(onChange1)
        controller.value = "change"
        assertEquals(1, called)

        val onChange2 = { _: Any?, _: Any? -> called++; Unit }
        controlled.addChangeListener(onChange2)
        controller.value = "cone more time"
        assertEquals(3, called)

        val onChange3 = object : (Any?, Any?) -> Unit {
            override fun invoke(p1: Any?, p2: Any?) {
                controlled.removeChangeListener(this)
            }
        }
        controlled.addChangeListener(onChange3)
        controller.value = "he-he"
        assertEquals(5, called)
    }

}
