package net.aquadc.persistence.android

import android.util.JsonToken
import net.aquadc.persistence.android.json.json
import net.aquadc.persistence.android.json.tokens
import net.aquadc.persistence.android.json.writeTo
import net.aquadc.persistence.extended.tokens.MergeStrategy
import net.aquadc.persistence.extended.tokens.associate
import net.aquadc.persistence.extended.tokens.entries
import net.aquadc.persistence.extended.tokens.inline
import net.aquadc.persistence.extended.tokens.outline
import net.aquadc.persistence.tokens.Index
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


val associated = """{"q":"y","w":2,"e":{"zzz":["xxx"]},"r":[1,2,3]}"""
val entries = "[" + // 1
        """{"k":"q","v":"y"},""" + // 19
        """{"k":"w","v":2},""" +
        """{"k":"e","v":{"zzz":["xxx"]}},""" +
        """{"k":"r","v":[1,2,3]}""" +
        "]"
val tuples = """[["q","y"],["w",2],["e",{"zzz":["xxx"]}],["r",[1,2,3]]]"""
val flippedTuples = """[["y","q"],[2,"w"],[{"zzz":["xxx"]},"e"],[[1,2,3],"r"]]"""

class TransformTest {
    @Test fun outline() =
            assertEquals(outlined, StringWriter().also { flat.reader().json().tokens().outlined().writeTo(it.json()) }.toString())

    @Test fun inline() =
            assertEquals(inlined, StringWriter().also { outlined.reader().json().tokens().inlined().writeTo(it.json()) }.toString())

    @Test fun `outline+inline`() =
            assertEquals(inlined, StringWriter().also { flat.reader().json().tokens().outlined().inlined().writeTo(it.json()) }.toString())


    @Test fun associate() =
            assertEquals(associated, StringWriter().also {
                entries.reader().json().tokens().associate(emptyArray(), "k", "v").writeTo(it.json())
            }.toString())

    @Test fun entries() =
            assertEquals(entries, StringWriter().also {
                associated.reader().json().tokens().entries(emptyArray(), "k", "v").writeTo(it.json())
            }.toString())

    @Test fun `associate+entries`() =
            assertEquals(associated, StringWriter().also {
                associated.reader().json().tokens().entries(emptyArray(), "k", "v").associate(emptyArray(), "k", "v").writeTo(it.json())
            }.toString())

    @Test fun associateTuples() =
            assertEquals(associated, StringWriter().also {
                tuples.reader().json().tokens().associate(emptyArray(), Index.First, Index.Second).writeTo(it.json())
            }.toString())

    @Test fun associateFlippedTuples() =
            assertEquals(associated, StringWriter().also {
                flippedTuples.reader().json().tokens().associate(emptyArray(), Index.Second, Index.First).writeTo(it.json())
            }.toString())

    @Test fun tuples() =
            assertEquals(tuples, StringWriter().also {
                associated.reader().json().tokens().entries(emptyArray(), Index.First, Index.Second).writeTo(it.json())
            }.toString())

    @Test fun `associateTuples+entries`() =
            assertEquals(associated, StringWriter().also {
                associated.reader().json().tokens()
                        .entries(emptyArray(), Index.First, Index.Second)
                        .associate(emptyArray(), Index.First, Index.Second).writeTo(it.json())
            }.toString())

    @Test fun flip() {
        assertEquals(
            """[["q","y"],["w",2],["e",true],["r",null]]""",
            StringWriter().also {
                """[["y","q"],[2,"w"],[true,"e"],[null,"r"]]""".reader().json().tokens()
                    .associate(emptyArray(), Index.First, Index.Second)
                    .entries(emptyArray(), Index.Second, Index.First)
                    .writeTo(it.json())
            }.toString()
        )
        assertEquals(
            """[["q","y"],["w",2],["e",true],["r",null]]""",
            StringWriter().also {
                """[["y","q"],[2,"w"],[true,"e"],[null,"r"]]""".reader().json().tokens()
                    .associate(emptyArray(), Index.Second, Index.First)
                    .entries(emptyArray(), Index.First, Index.Second)
                    .writeTo(it.json())
            }.toString()
        )
    }
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


    @Test fun associate() =
            assertStreamsEqual(associated, entries.reader().json().tokens().associate(emptyArray(), "k", "v"))

    @Test fun entries() =
            assertStreamsEqual(entries, associated.reader().json().tokens().entries(emptyArray(), "k", "v"))

    @Test fun `associate+entries`() =
            assertStreamsEqual(
                    entries,
                    entries.reader().json().tokens()
                            .associate(emptyArray(), "k", "v")
                            .entries(emptyArray(), "k", "v")
            )

    @Test fun associateTuples() =
            assertStreamsEqual(associated, tuples.reader().json().tokens().associate(emptyArray(), Index.First, Index.Second))

    @Test fun associateFlippedTuples() =
            assertStreamsEqual(associated, flippedTuples.reader().json().tokens().associate(emptyArray(), Index.Second, Index.First))

    @Test fun tuples() =
            assertStreamsEqual(tuples, associated.reader().json().tokens().entries(emptyArray(), Index.First, Index.Second))

    @Test fun `associateTuples+entries`() =
            assertStreamsEqual(
                    tuples,
                    tuples.reader().json().tokens()
                            .associate(emptyArray(), Index.First, Index.Second)
                            .entries(emptyArray(), Index.First, Index.Second)
            )

    @Test fun `entries backwards`() {
        assertStreamsEqual(
            """[["y","q"],[2,"w"],[true,"e"],[null,"r"]]""",
            """{"q":"y","w":2,"e":true,"r":null}""".reader().json().tokens()
                .entries(emptyArray(), Index.Second, Index.First)
        )
    }

    @Test fun `flip a`() {
        assertStreamsEqual(
            """[["q","y"],["w",2],["e",true],["r",null]]""",
            """[["y","q"],[2,"w"],[true,"e"],[null,"r"]]""".reader().json().tokens()
                    .associate(emptyArray(), Index.First, Index.Second)
                    .entries(emptyArray(), Index.Second, Index.First)
        )
    }

    @Test fun `flip b`() {
        assertStreamsEqual(
            """[["q","y"],["w",2],["e",true],["r",null]]""",
            """[["y","q"],[2,"w"],[true,"e"],[null,"r"]]""".reader().json().tokens()
                    .associate(emptyArray(), Index.Second, Index.First)
                    .entries(emptyArray(), Index.First, Index.Second)
        )
    }

    // gonna trigger different scenarios because both poll() and peek() could change TokenStream internal state
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
                    if (exReader.peek() == JsonToken.NAME) { exTokens.poll(); actual.poll() } else { exTokens.skipValue(); actual.skipValue() }
                    // don't skip names, this leads to nondeterministic paths
                    assertEquals(exTokens.path, actual.path)
                }
                2 -> {
                    assertEquals(exTokens.poll(), actual.poll())
                    assertEquals(exTokens.path, actual.path)
                }
                3 -> {
                    if (exReader.peek() == JsonToken.NAME) { exTokens.poll(); actual.poll() } else { exTokens.skipValue(); actual.skipValue() }
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
