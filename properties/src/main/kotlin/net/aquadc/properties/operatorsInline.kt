@file:JvmName("PropertyOperatorsInline")
@file:Suppress("NOTHING_TO_INLINE")

package net.aquadc.properties

import net.aquadc.properties.function.Arithmetic
import net.aquadc.properties.function.`BoolFunc-`
import kotlin.reflect.KProperty

// for simplicity, let 'and', 'or', 'xor' be here, too

//
// Boolean(s) -> Boolean
//

/**
 * Returns inverted view on this property.
 */
@Suppress("UNCHECKED_CAST")
inline operator fun Property<Boolean>.not(): Property<Boolean> =
        map(`BoolFunc-`.And /* any `BoolFunc-` instance represents unary NOT */)

/**
 * Returns a view on [this] && [that].
 */
@Suppress("UNCHECKED_CAST")
inline infix fun Property<Boolean>.and(that: Property<Boolean>): Property<Boolean> =
        mapWith(that, `BoolFunc-`.And)

/**
 * Returns a view on [this] || [that].
 */
@Suppress("UNCHECKED_CAST")
inline infix fun Property<Boolean>.or(that: Property<Boolean>): Property<Boolean> =
        mapWith(that, `BoolFunc-`.Or)

/**
 * Returns a view on [this] ^ [that].
 */
@Suppress("UNCHECKED_CAST")
inline infix fun Property<Boolean>.xor(that: Property<Boolean>): Property<Boolean> =
        mapWith(that, `BoolFunc-`.Xor)


//
// Number(s) -> Number
//

/**
 * Returns a [Property] which has value equal to sum of [this] and [that] values.
 */
@Suppress("UNCHECKED_CAST") @JvmName("plusInt")
inline operator fun Property<Int>.plus(that: Property<Int>): Property<Int> =
        mapWith(that, Arithmetic.Plus as (Int, Int) -> Int)

/**
 * Returns a [Property] which has value equal to sum of [this] and [that] values.
 */
@Suppress("UNCHECKED_CAST") @JvmName("plusLong")
inline operator fun Property<Long>.plus(that: Property<Long>): Property<Long> =
        mapWith(that, Arithmetic.Plus as (Long, Long) -> Long)

/**
 * Returns a [Property] which has value equal to sum of [this] and [that] values.
 */
@Suppress("UNCHECKED_CAST") @JvmName("plusFloat")
inline operator fun Property<Float>.plus(that: Property<Float>): Property<Float> =
        mapWith(that, Arithmetic.Plus as (Float, Float) -> Float)

/**
 * Returns a [Property] which has value equal to sum of [this] and [that] values.
 */
@Suppress("UNCHECKED_CAST") @JvmName("plusDouble")
inline operator fun Property<Double>.plus(that: Property<Double>): Property<Double> =
        mapWith(that, Arithmetic.Plus as (Double, Double) -> Double)

/**
 * Returns a [Property] which has value equal to difference of [this] and [that] values.
 */
@Suppress("UNCHECKED_CAST") @JvmName("minusInt")
inline operator fun Property<Int>.minus(that: Property<Int>): Property<Int> =
        mapWith(that, Arithmetic.Minus as (Int, Int) -> Int)

/**
 * Returns a [Property] which has value equal to difference of [this] and [that] values.
 */
@Suppress("UNCHECKED_CAST") @JvmName("minusLong")
inline operator fun Property<Long>.minus(that: Property<Long>): Property<Long> =
        mapWith(that, Arithmetic.Minus as (Long, Long) -> Long)

/**
 * Returns a [Property] which has value equal to difference of [this] and [that] values.
 */
@Suppress("UNCHECKED_CAST") @JvmName("minusFloat")
inline operator fun Property<Float>.minus(that: Property<Float>): Property<Float> =
        mapWith(that, Arithmetic.Minus as (Float, Float) -> Float)

/**
 * Returns a [Property] which has value equal to difference of [this] and [that] values.
 */
@Suppress("UNCHECKED_CAST") @JvmName("minusDouble")
inline operator fun Property<Double>.minus(that: Property<Double>): Property<Double> =
        mapWith(that, Arithmetic.Minus as (Double, Double) -> Double)

/**
 * Returns a [Property] which has value equal to product of [this] and [that] values.
 */
@Suppress("UNCHECKED_CAST") @JvmName("timesInt")
inline operator fun Property<Int>.times(that: Property<Int>): Property<Int> =
        mapWith(that, Arithmetic.Times as (Int, Int) -> Int)

/**
 * Returns a [Property] which has value equal to product of [this] and [that] values.
 */
@Suppress("UNCHECKED_CAST") @JvmName("timesLong")
inline operator fun Property<Long>.times(that: Property<Long>): Property<Long> =
        mapWith(that, Arithmetic.Times as (Long, Long) -> Long)

/**
 * Returns a [Property] which has value equal to product of [this] and [that] values.
 */
@Suppress("UNCHECKED_CAST") @JvmName("timesFloat")
inline operator fun Property<Float>.times(that: Property<Float>): Property<Float> =
        mapWith(that, Arithmetic.Times as (Float, Float) -> Float)

/**
 * Returns a [Property] which has value equal to product of [this] and [that] values.
 */
@Suppress("UNCHECKED_CAST") @JvmName("timesDouble")
inline operator fun Property<Double>.times(that: Property<Double>): Property<Double> =
        mapWith(that, Arithmetic.Times as (Double, Double) -> Double)

/**
 * Returns a [Property] which has value equal to quotient of [this] and [that] values.
 */
@Suppress("UNCHECKED_CAST") @JvmName("divInt")
inline operator fun Property<Int>.div(that: Property<Int>): Property<Int> =
        mapWith(that, Arithmetic.Div as (Int, Int) -> Int)

/**
 * Returns a [Property] which has value equal to quotient of [this] and [that] values.
 */
@Suppress("UNCHECKED_CAST") @JvmName("divLong")
inline operator fun Property<Long>.div(that: Property<Long>): Property<Long> =
        mapWith(that, Arithmetic.Div as (Long, Long) -> Long)

/**
 * Returns a [Property] which has value equal to quotient of [this] and [that] values.
 */
@Suppress("UNCHECKED_CAST") @JvmName("divFloat")
inline operator fun Property<Float>.div(that: Property<Float>): Property<Float> =
        mapWith(that, Arithmetic.Div as (Float, Float) -> Float)

/**
 * Returns a [Property] which has value equal to quotient of [this] and [that] values.
 */
@Suppress("UNCHECKED_CAST") @JvmName("divDouble")
inline operator fun Property<Double>.div(that: Property<Double>): Property<Double> =
        mapWith(that, Arithmetic.Div as (Double, Double) -> Double)


//
// Delegates
//

/**
 * Returns value of this [Property] when used as a Kotlin property delegate.
 */
inline operator fun <T> Property<T>.getValue(thisRef: Any?, property: KProperty<*>): T
        = value

/**
 * Sets value of this [MutableProperty] when used as a Kotlin property delegate.
 */
inline operator fun <T> MutableProperty<T>.setValue(thisRef: Any?, property: KProperty<*>, value: T) {
    this.value = value
}
