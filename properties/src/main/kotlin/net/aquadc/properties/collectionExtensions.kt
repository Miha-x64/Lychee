@file:JvmName("CollectionProperties")
package net.aquadc.properties


/**
 * Assigns `this.value + newMapping` to current property's value.
 */
operator fun <K, V> MutableProperty<Map<K, V>>.plusAssign(newMapping: Pair<K, V>) {
    do {
        val map = value
        val newMap = map + newMapping
        if (newMap === map) return // already contains this mapping
    } while (!casValue(map, newMap))
}

/**
 * Assigns `this.value + newElement` to current property's value.
 */
operator fun <T> MutableProperty<List<T>>.plusAssign(newElement: T) {
    do {
        val list = value
    } while (!casValue(list, list + newElement))
}


// TODO: minusAssign, etc
// TODO: move collection extensions to separate module, maybe along with `listOf(...).map { ... }`
