import com.sun.javafx.application.PlatformImpl
import net.aquadc.properties.DebounceTest
import net.aquadc.properties.fx.JavaFxApplicationThreadExecutorFactory
import org.junit.AssumptionViolatedException
import org.junit.Test

class FxDebounceTest {

    @Test
    fun onFxThread() {
        try {
            PlatformImpl.startup { }
        } catch (e: UnsupportedOperationException) {
            throw AssumptionViolatedException("Can't run JavaFX here.", e)
        }
        DebounceTest().test(JavaFxApplicationThreadExecutorFactory)
    }

}
