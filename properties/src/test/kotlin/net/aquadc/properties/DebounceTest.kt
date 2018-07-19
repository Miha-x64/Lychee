package net.aquadc.properties

import com.sun.javafx.application.PlatformImpl
import net.aquadc.properties.fx.JavaFxApplicationThreadExecutorFactory
import org.junit.Assert.*
import org.junit.AssumptionViolatedException
import org.junit.Test
import java.util.concurrent.Executor
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference


class DebounceTest {

    @Test(expected = UnsupportedOperationException::class)
    fun cantDebounceConcOnJavaAppMainThread() {
        concurrentPropertyOf("")
                .debounced(300, TimeUnit.MILLISECONDS)
                .addChangeListener { _, _ ->  }
    }

    @Test(expected = UnsupportedOperationException::class)
    fun cantDebounceUnsOnJavaAppMainThread() {
        propertyOf("")
                .debounced(300, TimeUnit.MILLISECONDS)
                .addChangeListener { _, _ ->  }
    }

    @Test fun onFJ() {
        test(ForkJoinPool(1))
    }

    fun test(executor: Executor) {
        val conc = concurrentPropertyOf("old")
        val concDeb = conc.debounced(100, TimeUnit.MILLISECONDS)

        val unsDeb = AtomicReference<Property<String>>()
        val concCalled = AtomicBoolean()
        val unsCalled = AtomicBoolean()
        executor.execute {
            val uns = propertyOf("old")
            unsDeb.set(uns.debounced(100, TimeUnit.MILLISECONDS))

            concDeb.addChangeListener { old, new ->
                assertEquals("old", old)
                assertEquals("new", new)
                concCalled.set(true)
            }
            unsDeb.get().addChangeListener { old, new ->
                assertEquals("old", old)
                assertEquals("new", new)
                unsCalled.set(true)
            }

            conc.value = "q"
            assertEquals("old", concDeb.value)
            conc.value = "w"
            assertEquals("old", concDeb.value)
            conc.value = "e"
            assertEquals("old", concDeb.value)

            uns.value = "q"
            assertEquals("old", unsDeb.get().value)
            uns.value = "w"
            assertEquals("old", unsDeb.get().value)
            uns.value = "e"
            assertEquals("old", unsDeb.get().value)

            Thread.sleep(10)
            conc.value = "new"
            assertEquals("old", concDeb.value)
            uns.value = "new"
            assertEquals("old", unsDeb.get().value)
        }

        Thread.sleep(10)
        conc.value = "r"
        assertEquals("old", concDeb.value)
        conc.value = "t"
        assertEquals("old", concDeb.value)
        conc.value = "y"
        assertEquals("old", concDeb.value)
        conc.value = "new"
        assertEquals("old", concDeb.value)

        assertFalse(concCalled.get())
        assertFalse(unsCalled.get())
        Thread.sleep(200)

        executor.execute {
            assertTrue(unsCalled.get())
            assertEquals("new", unsDeb.get().value)
            unsDeb.set(null)
        }

        Thread.sleep(1000)

        assertNull("failed inside of executor", unsDeb.get())

        assertTrue(concCalled.get())
        assertEquals("new", concDeb.value)
    }

}
