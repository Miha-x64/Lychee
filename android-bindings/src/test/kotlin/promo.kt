@file:Suppress("unused", "PublicApiImplicitType")

import android.content.SharedPreferences
import android.util.JsonReader
import android.widget.TextView
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.StructSnapshot
import net.aquadc.persistence.struct.build
import net.aquadc.persistence.type.int
import net.aquadc.persistence.type.string
import net.aquadc.properties.android.bindings.widget.bindTextTo
import net.aquadc.properties.android.persistence.json.read
import net.aquadc.properties.android.persistence.pref.SharedPreferencesStruct
import net.aquadc.properties.function.CharSequencez
import net.aquadc.properties.map
import java.io.StringReader


object Player : Schema<Player>() {
    val Name = "name" let string
    val Surname = "surname" let string
    val Score = "score".mut(int, default = 0)
}

val inMemoryPlayer: StructSnapshot<Player> = Player.build { p ->
    p[Name] = "John"
    p[Surname] = "Galt"
}

val jsonPlayer = JsonReader(StringReader(
        """{"name":"Hank","surname":"Rearden"}"""
)).read(Player)

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
