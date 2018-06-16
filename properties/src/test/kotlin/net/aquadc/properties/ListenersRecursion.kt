package net.aquadc.properties

import org.junit.Test
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ListenersRecursion {

    @Test fun mapUnderWLockCausingDeadlock() {
        val pool = Executors.newCachedThreadPool()
        val prop = concurrentPropertyOf(0)
        pool.submit {
            lateinit var mapped: Property<Int>
            mapped = prop.map {
                pool.submit {
                    mapped.addUnconfinedChangeListener { _, _ -> }
                }.get(10, TimeUnit.MINUTES)
                10 * it }
            mapped.addUnconfinedChangeListener { _, _ ->  }
        }.get(100, TimeUnit.MINUTES)
        pool.shutdown()
    }

    @Test fun nestedMap() {
        val pool = Executors.newCachedThreadPool()
        val prop = concurrentPropertyOf(0)
        pool.submit {
            lateinit var mapped: Property<Int>
            mapped = prop.map {
                mapped.addUnconfinedChangeListener { _, _ -> }
                10 * it }
            mapped.addUnconfinedChangeListener { _, _ ->  }
        }.get(100, TimeUnit.MINUTES)
        pool.shutdown()
    }

}
