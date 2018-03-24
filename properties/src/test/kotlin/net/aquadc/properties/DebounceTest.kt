package net.aquadc.properties

import com.sun.javafx.application.PlatformImpl
import org.junit.Assert.*
import org.junit.Ignore
import org.junit.Test
import java.util.concurrent.TimeUnit


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

        var concCalled = false
        var unsCalled = false
        PlatformImpl.startup {
            val uns = unsynchronizedMutablePropertyOf("old")
            val unsDeb = uns.debounced(100, TimeUnit.MILLISECONDS)

            concDeb.addChangeListener { old, new ->
                assertEquals("old", old)
                assertEquals("new", new)
                concCalled = true
            }
            unsDeb.addChangeListener { old, new ->
                assertEquals("old", old)
                assertEquals("new", new)
                unsCalled = true
            }

            conc.value = "q"
            conc.value = "w"
            conc.value = "e"

            uns.value = "q"
            uns.value = "w"
            uns.value = "e"

            Thread.sleep(10)
            conc.value = "new"
            uns.value = "new"
        }

        Thread.sleep(10)
        conc.value = "r"
        conc.value = "t"
        conc.value = "y"
        conc.value = "new"

        assertFalse(concCalled)
        assertFalse(unsCalled)
        Thread.sleep(200)

        assertTrue(concCalled)
        assertTrue(unsCalled)
    }

}
