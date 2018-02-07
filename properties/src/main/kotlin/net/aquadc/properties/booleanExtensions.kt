@file:Suppress("NOTHING_TO_INLINE")
package net.aquadc.properties


inline operator fun Property<Boolean>.not() =
        map(Not)

inline infix fun Property<Boolean>.and(that: Property<Boolean>): Property<Boolean> =
        mapWith(that, And)

inline infix fun Property<Boolean>.or(that: Property<Boolean>): Property<Boolean> =
        mapWith(that, Or)

inline infix fun Property<Boolean>.xor(that: Property<Boolean>): Property<Boolean> =
        mapWith(that, Xor)


inline fun MutableProperty<Boolean>.set() { value = true }

inline fun MutableProperty<Boolean>.clear() { value = false }

/**
 * Every time property becomes set (`true`),
 * it will be unset (`false`) and [action] will be performed.
 */
inline fun MutableProperty<Boolean>.clearEachAnd(crossinline action: () -> Unit) =
        addChangeListener { wasSet, isSet ->
            if (!wasSet && isSet) {
                value = false
                action()
            }
        }


// if we'd just use lambdas, they'd be copied into call-site

@PublishedApi internal object Not : (Boolean) -> Boolean {
    override fun invoke(p1: Boolean): Boolean = !p1
}
@PublishedApi internal object And : (Boolean, Boolean) -> Boolean {
    override fun invoke(p1: Boolean, p2: Boolean): Boolean = p1 && p2
}
@PublishedApi internal object Or : (Boolean, Boolean) -> Boolean {
    override fun invoke(p1: Boolean, p2: Boolean): Boolean = p1 || p2
}
@PublishedApi internal object Xor : (Boolean, Boolean) -> Boolean {
    override fun invoke(p1: Boolean, p2: Boolean): Boolean = p1 xor p2
}
