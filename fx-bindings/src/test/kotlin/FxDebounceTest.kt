import com.sun.javafx.application.PlatformImpl
import net.aquadc.properties.Property
import net.aquadc.properties.concurrentPropertyOf
import net.aquadc.properties.debounced
import net.aquadc.properties.fx.JavaFxApplicationThreadExecutorFactory
import net.aquadc.properties.propertyOf
import org.junit.Assert
import org.junit.AssumptionViolatedException
import org.junit.Test
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class FxDebounceTest {

    @Test
    fun onFxThread() {
        try {
            PlatformImpl.startup { }
        } catch (e: UnsupportedOperationException) {
            throw AssumptionViolatedException("Can't run JavaFX here.", e)
        }
        test(JavaFxApplicationThreadExecutorFactory)
    }

    /**
     * Copy-paste of core's DebounceTest.test(), keep in sync
     */
    private fun test(executor: Executor) {
        val conc = concurrentPropertyOf("old")
        val concDeb = conc.debounced(100, TimeUnit.MILLISECONDS)

        val unsDeb = AtomicReference<Property<String>>()
        val concCalled = AtomicBoolean()
        val unsCalled = AtomicBoolean()
        executor.execute {
            val uns = propertyOf("old")
            unsDeb.set(uns.debounced(100, TimeUnit.MILLISECONDS))

            concDeb.addChangeListener { old, new ->
                Assert.assertEquals("old", old)
                Assert.assertEquals("new", new)
                concCalled.set(true)
            }
            unsDeb.get().addChangeListener { old, new ->
                Assert.assertEquals("old", old)
                Assert.assertEquals("new", new)
                unsCalled.set(true)
            }

            conc.value = "q"
            Assert.assertEquals("old", concDeb.value)
            conc.value = "w"
            Assert.assertEquals("old", concDeb.value)
            conc.value = "e"
            Assert.assertEquals("old", concDeb.value)

            uns.value = "q"
            Assert.assertEquals("old", unsDeb.get().value)
            uns.value = "w"
            Assert.assertEquals("old", unsDeb.get().value)
            uns.value = "e"
            Assert.assertEquals("old", unsDeb.get().value)

            Thread.sleep(10)
            conc.value = "new"
            Assert.assertEquals("old", concDeb.value)
            uns.value = "new"
            Assert.assertEquals("old", unsDeb.get().value)
        }

        Thread.sleep(10)
        conc.value = "r"
        Assert.assertEquals("old", concDeb.value)
        conc.value = "t"
        Assert.assertEquals("old", concDeb.value)
        conc.value = "y"
        Assert.assertEquals("old", concDeb.value)
        conc.value = "new"
        Assert.assertEquals("old", concDeb.value)

        Assert.assertFalse(concCalled.get())
        Assert.assertFalse(unsCalled.get())
        Thread.sleep(200)

        executor.execute {
            Assert.assertTrue(unsCalled.get())
            Assert.assertEquals("new", unsDeb.get().value)
            unsDeb.set(null)
        }

        Thread.sleep(1000)

        Assert.assertNull("failed inside of executor", unsDeb.get())

        Assert.assertTrue(concCalled.get())
        Assert.assertEquals("new", concDeb.value)
    }

}
