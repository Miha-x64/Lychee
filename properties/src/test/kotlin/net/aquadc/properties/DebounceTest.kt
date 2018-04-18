package net.aquadc.properties

import com.sun.javafx.application.PlatformImpl
import javafx.application.Platform
import org.junit.Assert.*
import org.junit.Ignore
import org.junit.Test
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference


class DebounceTest {

    @Test(expected = UnsupportedOperationException::class)
    fun cantDebounceConcOnJavaAppMainThread() {
        concurrentMutablePropertyOf("")
                .debounced(300, TimeUnit.MILLISECONDS)
                .addChangeListener { _, _ ->  }
    }

    @Test(expected = UnsupportedOperationException::class)
    fun cantDebounceUnsOnJavaAppMainThread() {
        unsynchronizedMutablePropertyOf("")
                .debounced(300, TimeUnit.MILLISECONDS)
                .addChangeListener { _, _ ->  }
    }

    @Test @Ignore // JavaFX cannot start on CI
    fun onFxThread() {
        val conc = concurrentMutablePropertyOf("old")
        val concDeb = conc.debounced(100, TimeUnit.MILLISECONDS)

        val unsDeb = AtomicReference<Property<String>>()
        var concCalled = false
        var unsCalled = false
        PlatformImpl.startup {
            val uns = unsynchronizedMutablePropertyOf("old")
            unsDeb.set(uns.debounced(100, TimeUnit.MILLISECONDS))

            concDeb.addChangeListener { old, new ->
                assertEquals("old", old)
                assertEquals("new", new)
                concCalled = true
            }
            unsDeb.get().addChangeListener { old, new ->
                assertEquals("old", old)
                assertEquals("new", new)
                unsCalled = true
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

        assertFalse(concCalled)
        assertFalse(unsCalled)
        Thread.sleep(200)

        Platform.runLater {
            assertTrue(unsCalled)
            assertEquals("new", unsDeb.get().value)
            unsDeb.set(null)
        }

        Thread.sleep(100)

        assertNull("failed inside of Platform.runLater", unsDeb.get())

        assertTrue(concCalled)
        assertEquals("new", concDeb.value)
    }

}
