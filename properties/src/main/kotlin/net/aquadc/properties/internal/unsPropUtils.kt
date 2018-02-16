package net.aquadc.properties.internal

import net.aquadc.properties.ChangeListener

@Suppress("UNCHECKED_CAST")
internal fun <T> Any?.notifyAll(old: T, new: T) {
    when {
        this === null -> { /* no listeners, nothing to do */ }
        this is Function2<*, *, *> -> { (this as ChangeListener<T>)(old, new) }
        this.javaClass === ArrayList::class.java -> { (this as ArrayList<ChangeListener<T>>).toTypedArray().forEach { it(old, new) } }
        else -> throw AssertionError()
    }
}
