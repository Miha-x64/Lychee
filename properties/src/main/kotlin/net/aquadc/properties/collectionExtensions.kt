@file:JvmName("CollectionProperties")
package net.aquadc.properties

import kotlin.collections.HashSet


// Maps

/**
 * Assigns `this.value + newMapping` to current property's value.
 */
@JvmName("withMapping")
operator fun <K, V> MutableProperty<Map<K, V>>.plusAssign(newMapping: Pair<K, V>): Unit =
        update { map ->
            if (map.isEmpty()) mapOf(newMapping)
            else HashMap(map).also { it[newMapping.first] = newMapping.second }
        }

/**
 * Assigns `this.value - victimKey` to current property's value.
 */
@JvmName("withoutMapping")
operator fun <K, V> MutableProperty<Map<K, V>>.minusAssign(victimKey: K): Unit =
        update { map ->
            if (!map.containsKey(victimKey)) return

            when (map.size) {
             // 0 -> empty map won't contain anything — eliminated by 'containsKey()' check
                1 -> emptyMap()
                2 -> map.entries
                        .first { (key, _) -> key != victimKey }
                        .let { (k, v) -> java.util.Collections.singletonMap(k, v) }
                else -> HashMap(map).also { it.remove(victimKey) }
            }
        }

// Lists

/**
 * Assigns `this.value + newElement` to current property's value.
 */
@JvmName("listWthElement")
operator fun <T> MutableProperty<List<T>>.plusAssign(newElement: T): Unit =
        update { list ->
            when (list.size) {
                0 -> listOf(newElement)
                else -> list + newElement
            }
        }

// Sets

/**
 * Assigns `this.value + newElement` to current property's value.
 */
@JvmName("setWithElement")
operator fun <T> MutableProperty<Set<T>>.plusAssign(newElement: T): Unit =
    update { set ->
        if (set.contains(newElement)) return

        when (set.size) {
            0 -> setOf(newElement)
            else -> HashSet(set).also { it.add(newElement) }
        }
    }

/**
 * Assigns `this.value - newElement` to current property's value.
 */
@JvmName("setWithoutElement")
operator fun <T> MutableProperty<Set<T>>.minusAssign(victim: T): Unit =
    update { set ->
        if (!set.contains(victim)) return

        val size = set.size
        when (size) {
         // 0 -> empty set won't contain anything — eliminated by 'contains()' check
            1 -> emptySet()
            2 -> set.first { it != victim }.let(::setOf)
            else -> {
                var removed = false
                set.filterTo(HashSet(size-1)) {
                    val willRemove = !removed && it == victim
                    removed = removed or willRemove
                    !willRemove
                    // unreadable, (almost) branching-free code, yay!
                }
            }
        }
    }
