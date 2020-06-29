package net.aquadc.persistence

import android.annotation.SuppressLint
import androidx.annotation.RestrictTo
import java.util.Collections.newSetFromMap
import kotlin.collections.HashMap

private val andro: Boolean = try {
    android.os.Build.VERSION.SDK_INT >= 0; true
} catch (ignored: NoClassDefFoundError) {
    false
}

private val kitKat: Boolean = andro && try {
    android.os.Build.VERSION.SDK_INT >= 19
} catch (ignored: NoClassDefFoundError) {
    false
}

@Suppress("NOTHING_TO_INLINE")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
inline fun <K, V> newMap(): MutableMap<K, V> =
    newMap(0)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun <K, V> newMap(initialCapacity: Int): MutableMap<K, V> =
    if (kitKat) android.util.ArrayMap(initialCapacity)
    else HashMap(initialCapacity)

/*@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun <K, V> newMap(copyFrom: Map<K, V>): MutableMap<K, V> =
    if (kitKat) android.util.ArrayMap<K, V>(copyFrom.size).also { it.putAll(copyFrom) }
    else HashMap(copyFrom)*/

/*@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun <E> newSet(): MutableSet<E> =
    if (kitKat) Collections.newSetFromMap(android.util.ArrayMap(0))
    else HashSet()*/

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun <E> newSet(initialCapacity: Int): MutableSet<E> =
    if (kitKat) newSetFromMap(android.util.ArrayMap(initialCapacity))
    else HashSet(initialCapacity)

/*@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun <E> newSet(copyFrom: Collection<E>): MutableSet<E> =
    if (kitKat) Collections.newSetFromMap(android.util.ArrayMap<E, Boolean>(copyFrom.size)).also { it.addAll(copyFrom) }
    else HashSet(copyFrom)*/

@SuppressLint("NewApi") // false-positive: we won't use java.util.Base64 branch on Android
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun fromBase64(str: String): ByteArray =
    if (andro) android.util.Base64.decode(str, android.util.Base64.DEFAULT)
    else java.util.Base64.getDecoder().decode(str)

@SuppressLint("NewApi")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun toBase64(bytes: ByteArray): String =
    if (andro) android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
    else java.util.Base64.getEncoder().encodeToString(bytes)
