package net.aquadc.properties

import net.aquadc.properties.diff.calculateDiffOn
import net.aquadc.properties.executor.InPlaceWorker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.AssumptionViolatedException
import org.junit.Test
import java.util.concurrent.Callable
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.TimeUnit


class ContenededSubscription {

    private val pool = ForkJoinPool(1)

    @Test fun concProp() =
            contend(concurrentPropertyOf("")) { it }

    @Test fun contendConcBound() =
            contend(concurrentPropertyOf("")) { it }

    @Test fun contendConcMap() =
            contend(concurrentPropertyOf(""), Property<String>::readOnlyView)

    @Test fun contendConcBiMap() =
            contend(concurrentPropertyOf("")) { it.mapWith(concurrentPropertyOf("")) { a, b -> a + b } }

    @Test fun contendConcMultiMap() =
            contend(concurrentPropertyOf("")) { listOf(it).mapValueList { it.joinToString() } }

    @Test fun contendDiff() =
            contend(concurrentPropertyOf("")) { it.calculateDiffOn(InPlaceWorker) { _, _ -> "" } }

    @Test fun contendConcDebounced() {
        pool.submit {
            contend(concurrentPropertyOf("")) { it.debounced(0, TimeUnit.MILLISECONDS) }
        }.get()
    }

    @Test fun contendConcDistinct() =
            contend(concurrentPropertyOf("")) { it.distinct(Identity) }

    private fun contend(original: MutableProperty<String>, transform: (MutableProperty<String>) -> Property<String>) {
        if (Runtime.getRuntime().availableProcessors() < 2) {
            throw AssumptionViolatedException("this test makes sense only for multi-core machines")
        }

        var randomCalled by concurrentPropertyOf(false)
        var desiredCalled by concurrentPropertyOf(false)
        val desiredListener = { _: String, _: String -> desiredCalled = true }
        val listeners = MutableList(10_000) {
            if (it % 20 == 0) desiredListener
            else { _: String, _: String -> randomCalled = true }
        }
        assertNotSame(listeners[0], listeners[1])

        val prop = transform(original)
        ForkJoinPool.commonPool().invokeAll(List(Runtime.getRuntime().availableProcessors()) {
            Callable {
                listeners.forEach {
                    prop.addChangeListener(it)
                    Thread.yield()
                    prop.removeChangeListener(it)
                }
            }
        }).forEach { it.get() }

        original.value = "new"
        assertEquals(false, randomCalled)
        assertEquals(false, desiredCalled)

        prop.addUnconfinedChangeListener(desiredListener)
        original.value = "newer"
        assertEquals(false, randomCalled)
        Thread.sleep(10) // for debounced property
        assertEquals(true, desiredCalled)
    }

}
