package net.aquadc.properties

import org.junit.Assert.assertEquals
import org.junit.Test

class BindingTest {

    @Test fun binding() {
        val mutable = mutablePropertyOf("hello")
        val sample = mutablePropertyOf("world")

        mutable.bind(sample)
        assertEquals("world", mutable.value)

        sample.value = "goodbye"
        assertEquals("goodbye", mutable.value)

        mutable.value = "hey"
        sample.value = "unbound"
        assertEquals("hey", mutable.value)

        var new: String? = null
        sample.value = "just bound"
        mutable.addChangeListener { _, n -> new = n }
        mutable.bind(sample)
        assertEquals("just bound", new)

        sample.value = "rebound"
        assertEquals("rebound", mutable.value)

        val newSample = mutablePropertyOf("another")
        mutable.bind(newSample)
        assertEquals("another", mutable.value)

        sample.value = "bound to another"
        assertEquals("another", mutable.value)

        newSample.value = "bound to this"
        assertEquals("bound to this", mutable.value)
    }

    /**
     * Same as previous one, but immutable property binding implementation differs
     */
    @Test fun bindingToImmutable() {
        val mutable = mutablePropertyOf("hello")
        val immutable = immutablePropertyOf("world")

        mutable.bind(immutable)
        assertEquals("world", mutable.value)

        mutable.value = "hey"
        assertEquals("hey", mutable.value)
    }

}
