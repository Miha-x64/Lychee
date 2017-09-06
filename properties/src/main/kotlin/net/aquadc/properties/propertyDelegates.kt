@file:Suppress("NOTHING_TO_INLINE")
package net.aquadc.properties

import kotlin.reflect.KProperty

inline operator fun <T> Property<T>.getValue(thisRef: Any?, property: KProperty<*>): T {
    return value
}

inline operator fun <T> MutableProperty<T>.setValue(thisRef: Any?, property: KProperty<*>, value: T) {
    this.value = value
}
