package net.aquadc.persistence.android

import android.util.JsonToken
import net.aquadc.persistence.android.json.json
import net.aquadc.persistence.android.json.tokens
import net.aquadc.persistence.android.json.writeTo
import net.aquadc.persistence.extended.tokens.MergeStrategy
import net.aquadc.persistence.extended.tokens.inline
import net.aquadc.persistence.extended.tokens.outline
import net.aquadc.persistence.tokens.TokenStream
import net.aquadc.properties.function.Objectz
import net.aquadc.properties.function.isEqualTo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.StringWriter
import kotlin.random.Random


private val flat = "[" + // 1
        "{}," + // 4
        """{"a":"x"},""" + // 14
        """{"a":"x","ob":"y"},""" + // 33
        """{"a":"x","ob":"y","oc":"z"},""" + // 62
        """{"ob":"y","a":"x","oc":"z"},""" + // 90
        """{"oc":"z","ob":"y","a":"x"},""" + // 118
        """{"oc":[],"ob":{"x":"y"},"a":"x"},""" + // 151
        """{"oc":[{},[]],"ob":"y","a":"x"}""" + // 100
        "]"

private val outlined = "[" + // 1
        """{"out":{}},""" + // 12
        """{"a":"x","out":{}},""" + // 31
        """{"a":"x","out":{"b":"y"}},""" + // 57
        """{"a":"x","out":{"b":"y","c":"z"}},""" + // 91
        """{"a":"x","out":{"b":"y","c":"z"}},""" + // 125
        """{"a":"x","out":{"c":"z","b":"y"}},""" + // 159
        """{"a":"x","out":{"c":[],"b":{"x":"y"}}},""" + // 198
        """{"a":"x","out":{"c":[{},[]],"b":"y"}}""" + // 235
        "]"

val inlined = "[" + // 1
        """{},""" + // 4
        """{"a":"x"},""" + // 14
        """{"a":"x","ob":"y"},""" + // 33
        """{"a":"x","ob":"y","oc":"z"},""" + // 61
        """{"a":"x","ob":"y","oc":"z"},""" + // 89
        """{"a":"x","oc":"z","ob":"y"},""" + // 117
        """{"a":"x","oc":[],"ob":{"x":"y"}},""" + // 150
        """{"a":"x","oc":[{},[]],"ob":"y"}""" + // 181
        "]"

private fun TokenStream.outlined(): TokenStream =
        outline(
                path = arrayOf(Objectz.Any),
                what = { it == "ob" || it == "oc" },
                destName = "out",
                rename = { (it as String).substring(1) }
        )

private fun TokenStream.inlined(): TokenStream =
        inline(
                path = arrayOf(Objectz.Any),
                isVictim = isEqualTo("out"),
                rename = { "o$it" },
                merge = MergeStrategy.Fail
        )

class TransformTest {
    @Test fun outline() =
            assertEquals(outlined, StringWriter().also { flat.reader().json().tokens().outlined().writeTo(it.json()) }.toString())

    @Test fun inline() =
            assertEquals(inlined, StringWriter().also { outlined.reader().json().tokens().inlined().writeTo(it.json()) }.toString())

    @Test fun `outline+inline`() =
            assertEquals(inlined, StringWriter().also { flat.reader().json().tokens().outlined().inlined().writeTo(it.json()) }.toString())
}

@RunWith(Parameterized::class)
class TransformTestWParams(
        private val seed: Int
) {

    @Test fun outline() =
            assertStreamsEqual(outlined, flat.reader().json().tokens().outlined())

    @Test fun inline() =
            assertStreamsEqual(inlined, outlined.reader().json().tokens().inlined())

    @Test fun `outline+inline`() =
            assertStreamsEqual(inlined, flat.reader().json().tokens().outlined().inlined())

    // gonna trigger different scenarios because every method of TokenStream may change its internal state
    private fun assertStreamsEqual(expected: String, actual: TokenStream) {
        val r = Random(seed)
        val exReader = expected.reader().json()
        val exTokens = exReader.tokens()
        while (exTokens.hasNext()) {
            when (r.nextInt(4)) {
                0 -> {
                    assertEquals(exTokens.peek(), actual.peek())
                    assertEquals(exTokens.poll(), actual.poll())
                    assertEquals(exTokens.path, actual.path)
                }
                1 -> {
                    assertEquals(exTokens.peek(), actual.peek())
                    if (exReader.peek() == JsonToken.NAME) { exTokens.poll(); actual.poll() } else { exTokens.skip(); actual.skip() }
                    // don't skip names, this leads to nondeterministic paths
                    assertEquals(exTokens.path, actual.path)
                }
                2 -> {
                    assertEquals(exTokens.poll(), actual.poll())
                    assertEquals(exTokens.path, actual.path)
                }
                3 -> {
                    if (exReader.peek() == JsonToken.NAME) { exTokens.poll(); actual.poll() } else { exTokens.skip(); actual.skip() }
                    assertEquals(exTokens.path, actual.path)
                }
            }
        }
        assertFalse(actual.hasNext())
    }

    companion object {
        @JvmStatic @Parameterized.Parameters fun seeds() = 0..100
    }

}
