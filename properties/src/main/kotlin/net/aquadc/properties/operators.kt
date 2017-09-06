@file:Suppress("NOTHING_TO_INLINE")
package net.aquadc.properties

inline fun <T> Property<T>.readOnlyView() = map { it }
