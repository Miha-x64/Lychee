package net.aquadc.properties

import net.aquadc.properties.internal.*
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

    @Test fun concImmutablePropMapWithReturnsMapped() =
            immutablePropMapWithReturnsMapped(true, MappedProperty::class.java)

    @Test fun unsImmutablePropMapWithReturnsMapped() =
            immutablePropMapWithReturnsMapped(false, MappedProperty::class.java)

    private fun immutablePropMapWithReturnsMapped(concurrent: Boolean, mapsTo: Class<*>) {
        val prop0 = immutablePropertyOf("yo")
        val prop1 = propertyOf("hey", concurrent)

        val mapped0 = prop0.mapWith(prop1) { a, b -> "$b $a" }
        assertEquals("hey yo", mapped0.value)
        assertTrue(mapsTo.isInstance(mapped0))

        val mapped1 = prop1.mapWith(prop0) { a, b -> "$a $b" }
        assertEquals("hey yo", mapped1.value)
        assertTrue("mapped1 is ${mapped1.javaClass}", mapsTo.isInstance(mapped1))
    }

    @Test fun immutablePropMapWithImmutableReturnsImmutable() {
        val prop0 = immutablePropertyOf("hey")
        val prop1 = immutablePropertyOf("yo")

        val mapped = prop0.mapWith(prop1) { a, b -> "$a $b" }
        assertEquals("hey yo", mapped.value)
        assertTrue(mapped is ImmutableReferenceProperty)
    }

    @Test fun concSimpleMap() = simpleMap(true, MappedProperty::class.java)
    @Test fun unsSimpleMap() = simpleMap(false, MappedProperty::class.java)
    fun simpleMap(concurrent: Boolean, mapsTo: Class<*>) {
        val prop = propertyOf("hey", concurrent)
        val mapped = prop.map { "$it!" }
        assertTrue("mapped is ${mapped.javaClass}", mapsTo.isInstance(mapped))
    }

    @Test fun concSimpleMapWith() = simpleMapWith(true, BiMappedProperty::class.java)
    @Test fun unsSimpleMapWith() = simpleMapWith(false, BiMappedProperty::class.java)
    fun simpleMapWith(concurrent: Boolean, mapsTo: Class<*>) {
        val prop0 = propertyOf("hey", concurrent)
        val prop1 = propertyOf("hey", concurrent)
        assertTrue(mapsTo.isInstance(prop0.mapWith(prop1) { a, b -> "$a $b" }))
    }

    @Test fun mapValueList() {
        val prop0 = propertyOf("hey")
        val prop1 = concurrentPropertyOf("yo")
        val joinedProp = listOf(prop0, prop1).mapValueList { it.joinToString(" ") }
        assertEquals("hey yo", joinedProp.value)
        assertTrue(joinedProp is MultiMappedProperty<*, *>)
    }

    /*@Test fun concStressTest() {
        //  -Xmx10M

        // AtomicReference,    JDK 1.8, 42k, OOM: GC overhead limit exceeded
        // AtomicReference,    JDK   9, 56k, OOM: Java heap space

        // AtomicFieldUpdater, JDK 1.8, 51k, OOM: GC overhead limit exceeded
        // AtomicFieldUpdater, JDK   9, 70k, OOM: Java heap space
        val list = ArrayList<MutableProperty<Any?>>()
        while (true) {
            repeat(1_000) { list.add(concurrentMutablePropertyOf(null)) }
            println(list.size)
        }
    }*/

    /*@Test fun unsStressTest() {
        //  -Xmx10M

        // listeners: ArrayList,    JDK 1.8,  90k, OOM: GC overhead limit exceeded
        // listeners: ArrayList,    JDK   9, 103k, OOM: Java heap space

        // listeners: Any?,         JDK 1.8, 126k, OOM: GC overhead limit exceeded
        // listeners: Any?,         JDK   9, 124k, OOM: Java heap space
        val list = ArrayList<MutableProperty<Any?>>()
        while (true) {
            repeat(1_000) { list.add(unsynchronizedMutablePropertyOf(null)) }
            println(list.size)
        }
    }*/

}
