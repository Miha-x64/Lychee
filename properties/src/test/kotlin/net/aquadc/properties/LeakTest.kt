package net.aquadc.properties

import net.aquadc.properties.diff.calculateDiffOn
import net.aquadc.properties.executor.InPlaceWorker
import org.junit.Assert.assertEquals
import org.junit.Test
import java.lang.ref.WeakReference

class LeakTest {

    @Test fun definitelyWillPass() =
            leak(unsynchronizedMutablePropertyOf(""), { unsynchronizedMutablePropertyOf("") })

    @Test fun leakUnsMap() =
            leak(unsynchronizedMutablePropertyOf(""), Property<Any>::readOnlyView)

    @Test fun leakConcMap() =
            leak(concurrentMutablePropertyOf(""), Property<Any>::readOnlyView)

    @Test fun leakUnsBiMap() =
            leak(unsynchronizedMutablePropertyOf(""), { it.mapWith(unsynchronizedMutablePropertyOf("")) { a, b -> a + b } })

    @Test fun leakConcBiMap() =
            leak(concurrentMutablePropertyOf(""), { it.mapWith(concurrentMutablePropertyOf("")) { a, b -> a + b } })

    @Test fun leakUnsMultiMap() =
            leak(unsynchronizedMutablePropertyOf(""), { listOf(it).mapValueList { it } })

    @Test fun leakConcMultiMap() =
            leak(concurrentMutablePropertyOf(""), { listOf(it).mapValueList { it } })

    @Test fun leakDiff() =
            leak(unsynchronizedMutablePropertyOf(""), { it.calculateDiffOn(InPlaceWorker, { _, _ -> "" }) })

    private fun <T> leak(original: Property<T>, createForLeak: (Property<T>) -> Property<*>) {
        val ref1 = WeakReference(createForLeak(original))
        // should never hold new property
        assertGarbage(ref1)

        var killMePlease: Property<*>? = createForLeak(original)
        val listener = { _: Any?, _: Any? -> }
        killMePlease!!.addChangeListener(listener) // start holding new property
        val ref2 = WeakReference(killMePlease)
        killMePlease!!.removeChangeListener(listener) // stop holding [1]
        killMePlease = null // stop holding [2]
        assertGarbage(ref2)

        // hold the original property!
        println(original.toString().substring(0, 0))
    }

    private fun assertGarbage(ref: WeakReference<*>) {
        System.gc()
        assertEquals(null, ref.get())
    }

}
