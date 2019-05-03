package net.aquadc.persistence

import android.support.annotation.RestrictTo
import net.aquadc.persistence.type.AnyCollection
import java.util.Arrays
import java.util.Collections


@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun reallyEqual(a: Any?, b: Any?): Boolean = when {
    a == b -> true
    a === null || b === null -> false
    // popular array types
    a is Array<*> -> b is Array<*> && Arrays.equals(a, b)
    a is ByteArray -> b is ByteArray && Arrays.equals(a, b)
    a is IntArray -> b is IntArray && Arrays.equals(a, b)
    a is CharArray -> b is CharArray && Arrays.equals(a, b)
    // other array types
    a is BooleanArray -> b is BooleanArray && Arrays.equals(a, b)
    a is ShortArray -> b is ShortArray && Arrays.equals(a, b)
    a is LongArray -> b is LongArray && Arrays.equals(a, b)
    a is FloatArray -> b is FloatArray && Arrays.equals(a, b)
    a is DoubleArray -> b is DoubleArray && Arrays.equals(a, b)
    // just not equal and not arrays
    else -> false
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Any?.realHashCode(): Int = when (this) {
    null -> 0

    is Array<*> -> Arrays.deepHashCode(this)
    is ByteArray -> Arrays.hashCode(this)
    is IntArray -> Arrays.hashCode(this)
    is CharArray -> Arrays.hashCode(this)

    is BooleanArray -> Arrays.hashCode(this)
    is ShortArray -> Arrays.hashCode(this)
    is LongArray -> Arrays.hashCode(this)
    is FloatArray -> Arrays.hashCode(this)
    is DoubleArray -> Arrays.hashCode(this)

    else -> hashCode()
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Any?.realToString(): String = when (this) {
    null -> "null"

    is Array<*> -> Arrays.deepToString(this)
    is ByteArray -> Arrays.toString(this)
    is IntArray -> Arrays.toString(this)
    is CharArray -> Arrays.toString(this)

    is BooleanArray -> Arrays.toString(this)
    is ShortArray -> Arrays.toString(this)
    is LongArray -> Arrays.toString(this)
    is FloatArray -> Arrays.toString(this)
    is DoubleArray -> Arrays.toString(this)

    else -> toString()
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object New {

    private val kitKat: Boolean = try {
        android.os.Build.VERSION.SDK_INT >= 19
    } catch (ignored: NoClassDefFoundError) {
        false
    }

    fun <K, V> map(): MutableMap<K, V> =
            map(0)

    fun <K, V> map(initialCapacity: Int): MutableMap<K, V> =
            if (kitKat) android.util.ArrayMap(initialCapacity)
            else HashMap(initialCapacity)

    fun <K, V> map(copyFrom: Map<K, V>): MutableMap<K, V> =
            if (kitKat) android.util.ArrayMap<K, V>(copyFrom.size).also { it.putAll(copyFrom) }
            else HashMap(copyFrom)

    fun <E> set(): MutableSet<E> =
            if (kitKat) Collections.newSetFromMap(android.util.ArrayMap(0))
            else HashSet()

    fun <E> set(initialCapacity: Int): MutableSet<E> =
            if (kitKat) Collections.newSetFromMap(android.util.ArrayMap(initialCapacity))
            else HashSet(initialCapacity)

    fun <E> set(copyFrom: Collection<E>): MutableSet<E> =
            if (kitKat) Collections.newSetFromMap(android.util.ArrayMap<E, Boolean>(copyFrom.size)).also { it.addAll(copyFrom) }
            else HashSet(copyFrom)

}

internal inline fun <T, R> AnyCollection.fatMap(transform: (T) -> R): List<R> = when (this) {
    is List<*> -> Array<Any?>(size) { transform(this[it] as T) }
    is Collection<*> -> arrayOfNulls<Any>(size).also { dest ->
        forEachIndexed<Any?> { i, el -> dest[i] = transform(el as T) }
    }
    is Array<*> -> Array<Any?>(size) { transform(this[it] as T) }
    is ByteArray -> Array<Any?>(size) { transform(this[it] as T) }
    is ShortArray -> Array<Any?>(size) { transform(this[it] as T) }
    is IntArray -> Array<Any?>(size) { transform(this[it] as T) }
    is LongArray -> Array<Any?>(size) { transform(this[it] as T) }
    is FloatArray -> Array<Any?>(size) { transform(this[it] as T) }
    is DoubleArray -> Array<Any?>(size) { transform(this[it] as T) }
    else -> throw AssertionError()
}.asList() as List<R>

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
inline fun <C : MutableCollection<R>, T, R> AnyCollection.fatMapTo(dest: C, transform: (T) -> R): C = when (this) {
    is Collection<*> -> (this as Collection<T>).mapTo(dest, transform)
    is Array<*> -> (this as Array<T>).mapTo(dest, transform)
    is ByteArray -> this.mapTo(dest) { transform(it as T) }
    is ShortArray -> this.mapTo(dest) { transform(it as T) }
    is IntArray -> this.mapTo(dest) { transform(it as T) }
    is LongArray -> this.mapTo(dest) { transform(it as T) }
    is FloatArray -> this.mapTo(dest) { transform(it as T) }
    is DoubleArray -> this.mapTo(dest) { transform(it as T) }
    else -> throw AssertionError()
}

internal fun <C : MutableCollection<T>, T> AnyCollection.fatTo(dest: C): C {
    when (this) {
        is Collection<*> -> (this as Collection<T>).toCollection(dest)
        is Array<*> -> (this as Array<T>).toCollection(dest)
        is ByteArray -> this.toCollection(dest as MutableCollection<in Byte>)
        is ShortArray -> this.toCollection(dest as MutableCollection<in Short>)
        is IntArray -> this.toCollection(dest as MutableCollection<in Int>)
        is LongArray -> this.toCollection(dest as MutableCollection<in Long>)
        is FloatArray -> this.toCollection(dest as MutableCollection<in Float>)
        is DoubleArray -> this.toCollection(dest as MutableCollection<in Double>)
        else -> throw AssertionError()
    }
    return dest
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun <E> AnyCollection.fatAsList(): List<E> = when (this) {
    is List<*> -> this as List<E>
    is Collection<*> -> (this as Collection<E>).toList()
    is Array<*> -> (this as Array<E>).asList()
    is ByteArray -> this.asList() as List<E>
    is ShortArray -> this.asList() as List<E>
    is IntArray -> this.asList() as List<E>
    is LongArray -> this.asList() as List<E>
    is FloatArray -> this.asList() as List<E>
    is DoubleArray -> this.asList() as List<E>
    else -> throw AssertionError()
}
