package net.aquadc.persistence

import android.support.annotation.RestrictTo
import java.util.Arrays


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
