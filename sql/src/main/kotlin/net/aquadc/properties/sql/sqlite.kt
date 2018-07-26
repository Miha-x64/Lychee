package net.aquadc.properties.sql

import okio.ByteString
import java.util.*


val SQLite2Java: Map<String, Array<Class<out Any>>> = Collections.unmodifiableMap(mapOf(
        "INTEGER" to arrayOf(t<Boolean>(), t<Byte>(), t<Short>(), t<Int>(), t<Long>()),
        "REAL" to arrayOf(t<Float>(), t<Double>()),
        "TEXT" to arrayOf<Class<*>>(t<String>()),
        "BLOB" to arrayOf<Class<*>>(t<ByteString>()) // fixme potential NoClassDef
))

val Java2SQLite: Map<Class<out Any>, String> = SQLite2Java.let {
    val map = HashMap<Class<out Any>, String>()
    SQLite2Java.forEach { (sqlite, java) ->
        java.forEach { map[it] = sqlite }
    }
    Collections.unmodifiableMap(map)
}
