package net.aquadc.properties

import net.aquadc.properties.diff.calculateDiffOn
import net.aquadc.properties.executor.InPlaceWorker
import net.aquadc.properties.function.Objectz
import net.aquadc.properties.function.identity
import net.aquadc.properties.testing.assertGarbage
import org.junit.Test
import java.lang.ref.WeakReference
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.TimeUnit


class LeakTest {

    private val pool = ForkJoinPool(1)

    @Test fun definitelyWillPass() =
            leak(propertyOf("")) { propertyOf("") }

    @Test fun leakUnsBound() =
            leak(propertyOf("")) { propertyOf("").apply { bindTo(it) } }

    @Test fun leakConcBound() =
            leak(concurrentPropertyOf("")) { concurrentPropertyOf("").apply { bindTo(it) } }

    @Test fun leakUnsMap() =
            leak(propertyOf("")) { it.map(identity()) }

    @Test fun leakConcMap() =
            leak(concurrentPropertyOf("")) { it.map(identity()) }

    @Test fun leakUnsBiMap() =
            leak(propertyOf("")) { it.mapWith(propertyOf("")) { a, b -> a + b } }

    @Test fun leakConcBiMap() =
            leak(concurrentPropertyOf("")) { it.mapWith(concurrentPropertyOf("")) { a, b -> a + b } }

    @Test fun leakUnsMultiMap() =
            leak(propertyOf("")) { prop -> listOf(prop).mapValueList { it } }

    @Test fun leakConcMultiMap() =
            leak(concurrentPropertyOf("")) { prop -> listOf(prop).mapValueList { it } }

    @Test fun leakDiff() =
            leak(propertyOf("")) { it.calculateDiffOn(InPlaceWorker) { _, _ -> "" } }

    @Test fun leakUnsDebounced() {
        pool.submit {
            leak(propertyOf("")) { it.debounced(0, TimeUnit.MILLISECONDS) }
        }.get()
    }

    @Test fun leakConcDebounced() {
        pool.submit {
            leak(concurrentPropertyOf("")) { it.debounced(0, TimeUnit.MILLISECONDS) }
        }.get()
    }

    @Test fun leakUnsDistinct() =
            leak(propertyOf("")) { it.distinct(Objectz.Same) }

    @Test fun leakConcDistinct() =
            leak(concurrentPropertyOf("")) { it.distinct(Objectz.Same) }

    @Test fun leakUnsBidi() = leak(propertyOf("a")) { prop ->
        prop.bind({ "_$it" }, { it: String -> it.substring(1) })
    }

    @Test fun leakConcBidi() = leak(concurrentPropertyOf("a")) { prop ->
        prop.bind({ "_$it" }, { it: String -> it.substring(1) })
    }

    @Test fun leakUnsFlat() = leak(propertyOf("")) { prop ->
        prop.flatMap { immutablePropertyOf(it) }
    }

    @Test fun leakConcFlat() = leak(propertyOf("")) { prop ->
        prop.flatMap { immutablePropertyOf(it) }
    }

    private fun leak(original: MutableProperty<String>, createForLeak: (MutableProperty<String>) -> Property<*>) {
        val ref1 = WeakReference(createForLeak(original))
        // should never hold new property
        assertGarbage(ref1)

        var killMePlease: Property<*>? = createForLeak(original)
        val listener = { _: Any?, _: Any? -> }
        killMePlease!!.addUnconfinedChangeListener(listener) // start holding new property
        val ref2 = WeakReference(killMePlease)
        killMePlease.removeChangeListener(listener) // stop holding [1]
        @Suppress("UNUSED_VALUE")
        killMePlease = null // stop holding [2]
        assertGarbage(ref2)

        // hold the original property!
        original.value = "changed"
    }

}
