@file:Suppress("NOTHING_TO_INLINE")
package net.aquadc.properties

import net.aquadc.properties.internal.ConcDistinctPropertyWrapper
import net.aquadc.properties.internal.UnsDistinctPropertyWrapper

inline fun <T> Property<T>.readOnlyView() = map { it }

inline fun <T> Property<T>.distinct(noinline areEqual: (T, T) -> Boolean) = when {
    !this.mayChange -> this
    !isConcurrent -> UnsDistinctPropertyWrapper(this, areEqual)
    else -> ConcDistinctPropertyWrapper(this, areEqual)
}

object Identity : (Any?, Any?) -> Boolean {
    override fun invoke(p1: Any?, p2: Any?): Boolean = p1 === p2
}

object Equals : (Any?, Any?) -> Boolean {
    override fun invoke(p1: Any?, p2: Any?): Boolean = p1 == p2
}
