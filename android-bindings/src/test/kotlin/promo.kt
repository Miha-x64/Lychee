@file:Suppress("unused", "PublicApiImplicitType")

import android.content.SharedPreferences
import android.widget.TextView
import net.aquadc.persistence.android.json.json
import net.aquadc.persistence.android.json.tokens
import net.aquadc.persistence.android.json.writeTo
import net.aquadc.persistence.android.pref.SharedPreferencesStruct
import net.aquadc.persistence.extended.tokens.MergeStrategy
import net.aquadc.persistence.extended.tokens.inline
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.StructSnapshot
import net.aquadc.persistence.struct.invoke
import net.aquadc.persistence.tokens.readAs
import net.aquadc.persistence.type.i32
import net.aquadc.persistence.type.string
import net.aquadc.properties.android.bindings.widget.bindTextTo
import net.aquadc.properties.function.CharSequencez
import net.aquadc.properties.function.identity
import net.aquadc.properties.function.isEqualTo
import net.aquadc.properties.map
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.StringWriter


object Player : Schema<Player>() {
    val Name = "name" let string
    val Surname = "surname" let string
    val Score = "score".mut(i32, default = 0)
}

val inMemoryPlayer: StructSnapshot<Player> = Player { p ->
    p[Name] = "John"
    p[Surname] = "Galt"
}

val jsonPlayer = """{"name":"Hank","surname":"Rearden"}"""
        .reader().json()
        .tokens().readAs(Player)

val prefPlayer =
        SharedPreferencesStruct(jsonPlayer, getSharedPreferences())

val tv = getTextView().let { textView ->
    val scoreProp = (prefPlayer prop Player.Score)
            .map(CharSequencez.ValueOf)
    textView.bindTextTo(scoreProp)
}


private fun getSharedPreferences(): SharedPreferences =
        throw NotImplementedError()

private fun getTextView(): TextView =
        throw NotImplementedError()

class Transforms {

    @Test fun sample() = // note: lines 61-69 are linked from readme
            assertEquals(
                    """{"a":"whatever","b":"extra 1","c":"extra 2"}""",
                    StringWriter().also {
                        """{"a":"whatever","extras":{"b":"extra 1","c":"extra 2"}}"""
                                .reader().json().tokens()
                                .inline(arrayOf(), isEqualTo("extras"), identity(), MergeStrategy.Fail)
                                .writeTo(it.json())
                    }.toString()
            )

}
