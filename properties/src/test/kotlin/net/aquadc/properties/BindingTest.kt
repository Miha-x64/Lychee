package net.aquadc.properties

import net.aquadc.properties.executor.UnconfinedExecutor
import org.junit.Assert.*
import org.junit.Test


class BindingTest {

    @Test fun concBinding() = binding(true)
    @Test fun unsBinding() = binding(false)
    private fun binding(concurrent: Boolean) {
        val mutable = propertyOf("hello", concurrent)
        val sample = propertyOf("world", concurrent)

        mutable.bindTo(sample)
        assertEquals("world", mutable.value)

        sample.value = "goodbye"
        assertEquals("goodbye", mutable.value)

        mutable.value = "hey"
        sample.value = "unbound"
        assertEquals("hey", mutable.value)

        mutable.bindTo(sample)
        sample.value = "ignored"
        sample.value = "2"
        assertTrue(mutable.casValue("2", "ignored"))

        // let's check equality CAS, too
        assertTrue(mutable.casValue(StringBuilder("igno").append("red").toString(), "ignored2"))
        assertFalse(mutable.casValue(StringBuilder("igno").append("red").toString(), "ignored2"))

        var new: String? = null
        sample.value = "just bound"
        mutable.addChangeListenerOn(UnconfinedExecutor) { _, n -> new = n }
        mutable.bindTo(sample)
        mutable.bindTo(sample) // binding should be idempotent
        assertEquals("just bound", new)

        sample.value = "rebound"
        assertEquals("rebound", mutable.value)

        val newSample = propertyOf("another", concurrent)
        mutable.bindTo(newSample)
        assertEquals("another", mutable.value)

        sample.value = "bound to another"
        assertEquals("another", mutable.value)

        newSample.value = "bound to this"
        assertEquals("bound to this", mutable.value)

        assertFalse(mutable.casValue("bound to dis", "whatever"))
        assertTrue(mutable.casValue("bound to this", "whatever"))
        assertEquals("whatever", mutable.value)

        newSample.value = "should not propagate to 'mutable'"
        assertEquals("whatever", mutable.value)
    }

    @Test fun concBindingToImmutable() = bindingToImmutable(true)
    @Test fun unsBindingToImmutable() = bindingToImmutable(false)
    /**
     * Same as previous one, but immutable property binding implementation differs
     */
    private fun bindingToImmutable(concurrent: Boolean) {
        val mutable = propertyOf("hello", concurrent)
        val immutable = immutablePropertyOf("world")

        mutable.bindTo(immutable)
        assertEquals("world", mutable.value)

        mutable.value = "hey"
        assertEquals("hey", mutable.value)
    }

}
