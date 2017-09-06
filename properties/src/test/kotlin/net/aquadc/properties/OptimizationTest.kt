package net.aquadc.properties

import net.aquadc.properties.internal.ImmutableReferenceProperty
import net.aquadc.properties.internal.MappedProperty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OptimizationTest {

    @Test fun immutablePropMapReturnsImmutable() {
        val prop = immutablePropertyOf("yo")

        val mapped = prop.map { it + it }
        assertEquals("yoyo", mapped.value)
        assertTrue(mapped is ImmutableReferenceProperty)
    }

    @Test fun immutablePropMapWithReturnsMapped() {
        val prop0 = immutablePropertyOf("yo")
        val prop1 = mutablePropertyOf("hey")

        val mapped0 = prop0.mapWith(prop1) { a, b -> "$b $a" }
        assertEquals("hey yo", mapped0.value)
        assertTrue(mapped0 is MappedProperty<*, *>)

        val mapped1 = prop1.mapWith(prop0) { a, b -> "$a $b" }
        assertEquals("hey yo", mapped1.value)
        assertTrue("mapped1 is ${mapped1.javaClass}", mapped1 is MappedProperty<*, *>)
    }

    @Test fun immutablePropMapWithImmutableReturnsImmutable() {
        val prop0 = immutablePropertyOf("hey")
        val prop1 = immutablePropertyOf("yo")

        val mapped = prop0.mapWith(prop1) { a, b -> "$a $b" }
        assertEquals("hey yo", mapped.value)
        assertTrue(mapped is ImmutableReferenceProperty)
    }

}
