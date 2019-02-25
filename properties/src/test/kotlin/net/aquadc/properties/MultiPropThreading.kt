package net.aquadc.properties

import org.junit.Test
import java.util.concurrent.Executors
import java.util.concurrent.ForkJoinPool


class MultiPropThreading {

    @Test fun `bi-mapped`() =
            test { aProp, bProp ->
                aProp.mapWith(bProp) { a, b -> a + b }
            }

    @Test fun `multi-mapped`() =
            test { aProp, bProp ->
                listOf(aProp, bProp).mapValueList { (a, b) -> a + b }
            }

    private fun test(combine: (Property<Int>, Property<Int>) -> Property<Int>) {
        ForkJoinPool(1, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true).submit {
            val aProp = propertyOf(1)
            val bProp = concurrentPropertyOf(2)
            val bi = combine(aProp, bProp)
            bi.addChangeListener { _, _ -> // this will require subscription of bi's listener of bProp to be confined to current thread!
                bi.value // any touch from wrong thread will lead to instant crash
            }
            val ex = Executors.newSingleThreadExecutor()
            try {
                ex.submit { bProp.value = 100500 }.get() // assert can deal with multithreading
                if (bi.isConcurrent) ex.submit { bi.value }.get() // isConcurrent value is unspecified, but must be correct
            } finally {
                ex.shutdown()
            }
        }.get()
    }

}
