package net.aquadc.properties.testing

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import java.lang.ref.WeakReference


fun assertReturnsGarbage(func: () -> Any) {
    var v: Any? = func()
    val ref = WeakReference(v)
    @Suppress("UNUSED_VALUE")
    v = null
    assertGarbage(ref)
}

fun assertGarbage(ref: WeakReference<*>) {
    System.gc()
    if (ref.get() != null) {
        // this happens sometimes
        Thread.yield()
        System.gc()
        Thread.yield()
    }
    if (ref.get() !== null) fail("${ref.get()} was not garbage-collected")
}
