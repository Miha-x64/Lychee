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
        mutable.bindTo(sample) // binding should be idempotent
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

}
