package net.aquadc.properties

import net.aquadc.properties.diff.calculateDiffOn
import net.aquadc.properties.executor.InPlaceWorker
import net.aquadc.properties.function.Objectz
import net.aquadc.properties.function.identity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.AssumptionViolatedException
import org.junit.Test
import java.util.concurrent.Callable
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit


class ContenededSubscription {

    private val pool = ForkJoinPool(1)

    @Test fun concProp() =
            contend(concurrentPropertyOf("")) { it }

    @Test fun contendConcMap() =
            contend(concurrentPropertyOf("")) { it.map(identity()) }

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
            contend(concurrentPropertyOf("")) { it.distinct(Objectz.Same) }

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
                // iterate this list in parallel on every core
                listeners.forEach {
                    prop.addUnconfinedChangeListener(it)
                    Thread.yield()
                    prop.removeChangeListener(it)
                }
            }
        }).forEach(Future<Unit>::get)

        original.value = "new"
        assertEquals(false, randomCalled)
        assertEquals(false, desiredCalled)

        prop.addUnconfinedChangeListener(desiredListener)
        original.value = "newer"
        assertEquals(false, randomCalled)
        Thread.sleep(10) // for debounced property
        assertEquals(true, desiredCalled)
    }

    @Test fun unsObservedState() = observedState(false)
    @Test fun concObservedState() = observedState(true)
    @Test fun unsBiObservedState() = biObservedState(false)
    @Test fun concBiObservedState() = biObservedState(true)
    @Test fun unsListObservedState() = listObservedState(false)
    @Test fun concListObservedState() = listObservedState(true)

    private fun observedState(conc: Boolean) =
            observedState(propertyOf(0, conc)) { p, m -> p.map(m) }

    private fun biObservedState(conc: Boolean) =
            observedState(propertyOf(0, conc)) { p, m -> p.mapWith(propertyOf(-1, conc)) { v, _ -> m(v) } }

    private fun listObservedState(conc: Boolean) =
            observedState(propertyOf(0, conc)) { p, m -> listOf(p).mapValueList { m(it[0]) } }

    private fun observedState(original: MutableProperty<Int>, transform: (MutableProperty<Int>, mapper: (Int) -> Int) -> Property<Int>) {
        var mapperCalled = 0
        val prop = transform(original) { mapperCalled++ }

        assertEquals(0, mapperCalled)

        prop.value
        assertEquals(1, mapperCalled)

        val listener: ChangeListener<Int> = { _, _ -> }
        prop.addUnconfinedChangeListener(listener) // this will trigger value evaluation
        assertEquals(2, mapperCalled)

        prop.value // since value is already evaluated, nothing happens
        assertEquals(2, mapperCalled)

        original.value = 0
        assertEquals(3, mapperCalled)

        prop.value
        assertEquals(3, mapperCalled)

        prop.removeChangeListener(listener) // no one's listening, drop value

        prop.value // evaluate
        assertEquals(4, mapperCalled)

        prop.addUnconfinedChangeListener(object : ChangeListener<Int> {
            override fun invoke(old: Int, new: Int) {
                prop.removeChangeListener(listener) // I don't remember why I'm trying to remove nonexistent listener
                prop.removeChangeListener(this)
            }
        }) // subscribe & evaluate
        assertEquals(5, mapperCalled)

        original.value = 0
        assertEquals(6, mapperCalled)

        prop.value // this will also unsubscribe our listener from the above
        assertEquals(7, mapperCalled)

        prop.addUnconfinedChangeListener(object : ChangeListener<Int> {
            override fun invoke(old: Int, new: Int) {
                prop.addUnconfinedChangeListener(listener) // queued 'cause we're notifying now
                prop.removeChangeListener(this) // unobserved
                prop.removeChangeListener(listener) // unqueue
            }
        }) // evaluate
        assertEquals(8, mapperCalled)

        original.value = 0
        assertEquals(9, mapperCalled)

        prop.value
        assertEquals(10, mapperCalled)
    }

}
