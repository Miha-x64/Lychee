@file:Suppress("NOTHING_TO_INLINE")
package net.aquadc.properties

/**
 * Returns inverted view on this property.
 */
inline operator fun Property<Boolean>.not() =
        map(UnaryNotBinaryAnd)

/**
 * Returns a view on [this] && [that].
 */
inline infix fun Property<Boolean>.and(that: Property<Boolean>): Property<Boolean> =
        mapWith(that, UnaryNotBinaryAnd)

/**
 * Returns a view on [this] || [that].
 */
inline infix fun Property<Boolean>.or(that: Property<Boolean>): Property<Boolean> =
        mapWith(that, Or)

/**
 * Returns a view on [this] ^ [that].
 */
inline infix fun Property<Boolean>.xor(that: Property<Boolean>): Property<Boolean> =
        mapWith(that, Xor)

/**
 * Sets [this] value to `true`.
 */
inline fun MutableProperty<Boolean>.set() { value = true }


/**
 * Sets [this] value to `false`.
 */
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

@PublishedApi internal object UnaryNotBinaryAnd :
        /* not: */ (Boolean) -> Boolean,
        /* and: */ (Boolean, Boolean) -> Boolean {

    override fun invoke(p1: Boolean): Boolean = !p1
    override fun invoke(p1: Boolean, p2: Boolean): Boolean = p1 && p2
}
@PublishedApi internal object Or : (Boolean, Boolean) -> Boolean {
    override fun invoke(p1: Boolean, p2: Boolean): Boolean = p1 || p2
}
@PublishedApi internal object Xor : (Boolean, Boolean) -> Boolean {
    override fun invoke(p1: Boolean, p2: Boolean): Boolean = p1 xor p2
}
