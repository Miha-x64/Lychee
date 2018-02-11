package net.aquadc.properties.internal

import net.aquadc.properties.ChangeListener


internal fun checkThread(expected: Thread) {
    if (Thread.currentThread() !== expected)
        throw RuntimeException("${Thread.currentThread()} is not allowed to touch this property since it was created in $expected.")
}

/**
 * Notifies change listener(s).
 * If `null` passed, does nothing;
 * if [ChangeListener] passed, notifies it;
 * if [ArrayList] passed, notifies each listener;
 * throws an [AssertionError] otherwise.
 */
@Suppress("UNCHECKED_CAST")
internal fun <T> Any?.notifyAll(old: T, new: T) = when {
    this === null -> { /* no listeners, nothing to do */ }
    this is Function2<*, *, *> -> { (this as ChangeListener<T>)(old, new) }
    this.isArrayList() -> { (this as ArrayList<ChangeListener<T>>).forEach { it(old, new) } }
    else -> throw AssertionError()
}

@Suppress("UNCHECKED_CAST")
internal fun <T> Any?.plus(listener: ChangeListener<T>): Any = when {
    this === null -> listener
    this is Function2<*, *, *> -> ArrayList<ChangeListener<T>>(2).also { it.add(this as ChangeListener<T>); it.add(listener) }
    this.isArrayList() -> (this as ArrayList<ChangeListener<T>>).apply { add(listener) }
    else -> throw AssertionError()
}

@Suppress("UNCHECKED_CAST")
internal fun <T> Any?.minus(listener: ChangeListener<T>): Any? = when {
    this === null -> null
    this === listener -> null
    this is Function2<*, *, *> -> this
    this.isArrayList() -> (this as ArrayList<ChangeListener<T>>).apply { remove(listener) }
    else -> throw AssertionError()
}

@Suppress("NOTHING_TO_INLINE")
private inline fun Any.isArrayList() = javaClass === ArrayList::class.java
