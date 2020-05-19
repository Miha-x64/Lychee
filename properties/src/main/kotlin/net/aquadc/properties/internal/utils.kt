@file:JvmName("Internal\$Utils")
package net.aquadc.properties.internal

import androidx.annotation.RestrictTo
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater


internal fun Array<*>.with(lastElement: Any?): Array<Any?> {
    val size = size
    val new = arrayOfNulls<Any>(size + 1)
    System.arraycopy(this, 0, new, 0, size)
    new[size] = lastElement
    return new
}

internal fun Array<*>.copyOfWithout(idx: Int, canonicalEmptyArray: Array<Any?>?): Array<Any?>? {
    val oldSize = size
    if (idx == 0 && oldSize == 1) return canonicalEmptyArray

    val new = arrayOfNulls<Any>(oldSize-1)
    System.arraycopy(this, 0, new, 0, idx)
    System.arraycopy(this, idx+1, new, idx, oldSize-idx-1)
    return new
}

@Suppress("UNCHECKED_CAST")
internal fun <T : Any> Array<out T?>.withoutNulls(canonicalEmptyArray: Array<T>?): Array<T>? {
    val nulls = count { it == null }
    if (nulls == 0) return this as Array<T> // it safe since there are no actual nulls

    val newSize = size - nulls
    if (newSize == 0) return canonicalEmptyArray

    val newArray = arrayOfNulls<Any>(size - nulls)
    var destPos = 0
    forEach { if (it != null) newArray[destPos++] = it }

    return newArray as Array<T> // not actually T[], but it's OK
}

/**
 * Compacts array, where null means 'empty'.
 * @return index of first empty index after compaction, or -1, if none
 * @implNote copied from [java.util.ArrayList.batchRemove]
 */
internal fun <T> Array<T?>.compact(): Int {
    var head = 0
    var w = 0
    while (head < size) {
        if (this[head] != null)
            this[w++] = this[head]
        head++
    }
    for (i in w until size) {
        this[i] = null
    }
    return if (w == size) -1 else w
}

internal fun <T, V> AtomicReferenceFieldUpdater<T, V>.eagerOrLazySet(thisRef: T, confinedTo: Thread?, value: V) {
    if (confinedTo == null) set(thisRef, value)
    else lazySet(thisRef, value)
}

internal fun <T, V> AtomicReferenceFieldUpdater<T, V>.eagerOrLazyCas(thisRef: T, confinedTo: Thread?, expect: V, update: V): Boolean =
        if (confinedTo == null) compareAndSet(thisRef, expect, update)
        else lazySet(thisRef, update).let { true }

/**
 * Despite this class is public, it is a private API. Don't ever touch this. Never.
 * Seriously. You're were warned.
 */
@[JvmField JvmSynthetic RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)]
val Unset: Any = Any()

@[JvmField JvmSynthetic] internal val SingleNull = arrayOfNulls<Any>(1)

@[JvmField JvmSynthetic PublishedApi] internal val EmptyArray: Array<Any?> =
        emptyArray<Any?>()

@Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmSynthetic
inline fun <T> emptyArrayOf(): Array<T> =
        EmptyArray as Array<T>

@[JvmField JvmSynthetic] internal val NoListeners =
        ConcListeners<Any, Any?>(EmptyArray, EmptyArray, 0) as ConcListeners<Nothing, Nothing>

@[JvmField JvmSynthetic PublishedApi] internal val TRUE = `Immutable-`(true)
@[JvmField JvmSynthetic PublishedApi] internal val FALSE = `Immutable-`(false)
@[JvmField JvmSynthetic PublishedApi] internal val UNIT = `Immutable-`(Unit)

/**
 * Represents a function which can be un-applied.
 * For example, when `invoke(arg) = 10 * arg`, `backwards(arg) = arg / 10`.
 */
internal interface TwoWay<T, R> : (T) -> R {

    /**
     * Represents an action opposite to invoking this function.
     */
    fun backwards(arg: R): T
}

@PublishedApi internal inline fun <R, T> TwoWay(
    crossinline forward: (T) -> R, crossinline backwards: (R) -> T
): TwoWay<T, R> =
    object : TwoWay<T, R> {
        override fun invoke(p1: T): R = forward.invoke(p1)
        override fun backwards(arg: R): T = backwards.invoke(arg)
    }
