@file:Suppress("NOTHING_TO_INLINE")
@file:JvmName("PropertiesInline")
package net.aquadc.properties

import net.aquadc.properties.executor.UnconfinedExecutor
import net.aquadc.properties.internal.*
import java.util.concurrent.TimeUnit
import kotlin.reflect.KProperty


/**
 * Observer for [Property]<T>.
 */
typealias ChangeListener<T> = (old: T, new: T) -> Unit
// typealias just takes place in metadata, let it live with inline functions


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
// T(s) -> Boolean
//

/**
 * Returns a view on [this] == [that].
 */
inline fun <T> Property<T>.equalTo(that: Property<T>): Property<Boolean> =
        mapWith(that, ToBoolFunc2.Equality)

/**
 * Returns a view on [this] == [that].
 */
@Suppress("UNCHECKED_CAST")
inline fun <T> Property<T>.valueEqualTo(value: T): Property<Boolean> =
        map(`Contains-`<T>(value, 3) as (T) -> Boolean)

/**
 * Returns a view on `this === null`.
 */
inline fun <T : Any> Property<T?>.isNull(): Property<Boolean> =
        map(ToBoolFunc1.IsNull)

/**
 * Returns a view on `this !== null`.
 */
inline fun <T : Any> Property<T?>.isNotNull(): Property<Boolean> =
        map(ToBoolFunc1.IsNotNull)

/**
 * Compares objects by their identity.
 */
inline fun byIdentity(): (Any?, Any?) -> Boolean =
        ToBoolFunc2.Identity

/**
 * Compares objects with [Any.equals] operator function.
 */
inline fun byEquality(): (Any?, Any?) -> Boolean =
        ToBoolFunc2.Equality

/**
 * Compares [Boolean] arrays with [java.util.Arrays.equals].
 */
inline fun byBooleanArraysEquality(): (BooleanArray, BooleanArray) -> Boolean =
        ToBoolFunc2.Booleans

/**
 * Compares [Byte] arrays with [java.util.Arrays.equals].
 */
inline fun byByteArraysEquality(): (ByteArray, ByteArray) -> Boolean =
        ToBoolFunc2.Bytes

/**
 * Compares [Short] arrays with [java.util.Arrays.equals].
 */
inline fun byShortArraysEquality(): (ShortArray, ShortArray) -> Boolean =
        ToBoolFunc2.Shorts

/**
 * Compares [Char] arrays with [java.util.Arrays.equals].
 */
inline fun byCharArraysEquality(): (CharArray, CharArray) -> Boolean =
        ToBoolFunc2.Chars

/**
 * Compares [Int] arrays with [java.util.Arrays.equals].
 */
inline fun byIntArraysEquality(): (IntArray, IntArray) -> Boolean =
        ToBoolFunc2.Ints

/**
 * Compares [Long] arrays with [java.util.Arrays.equals].
 */
inline fun byLongArraysEquality(): (LongArray, LongArray) -> Boolean =
        ToBoolFunc2.Longs

/**
 * Compares [Float] arrays with [java.util.Arrays.equals].
 */
inline fun byFloatArraysEquality(): (FloatArray, FloatArray) -> Boolean =
        ToBoolFunc2.Floats

/**
 * Compares [Double] arrays with [java.util.Arrays.equals].
 */
inline fun byDoubleArraysEquality(): (DoubleArray, DoubleArray) -> Boolean =
        ToBoolFunc2.Doubles

/**
 * Compares reference arrays with [java.util.Arrays.equals].
 */
inline fun byArraysEquality(): (Array<out Any?>, Array<out Any?>) -> Boolean =
        ToBoolFunc2.Objects

/**
 * Compares reference arrays with [java.util.Arrays.deepEquals].
 */
inline fun byArraysDeepEquality(): (Array<out Any?>, Array<out Any?>) -> Boolean =
        ToBoolFunc2.ObjectsDeep


//
// Boolean actions
//

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
inline fun MutableProperty<Boolean>.clearEachAnd(crossinline action: () -> Unit): Unit = onEach {
    if (it) {
        value = false
        action()
    }
}


//
// CharSequence
//

/**
 * Returns a property representing [CharSequence.length].
 */
@Suppress("UNCHECKED_CAST")
inline val Property<CharSequence>.length: Property<Int> get() = map(`CharSeqFunc-`.Length)

/**
 * Returns a property representing emptiness ([CharSequence.isEmpty]).
 */
inline val Property<CharSequence>.isEmpty: Property<Boolean> get() = map(`CharSeqFunc-`.Empty)

/**
 * Returns a property representing non-emptiness ([CharSequence.isNotEmpty]).
 */
inline val Property<CharSequence>.isNotEmpty: Property<Boolean> get() = map(`CharSeqFunc-`.NotEmpty)

/**
 * Returns a property representing blankness ([CharSequence.isBlank]).
 */
inline val Property<CharSequence>.isBlank: Property<Boolean> get() = map(`CharSeqFunc-`.Blank)

/**
 * Returns a property representing non-blankness ([CharSequence.isNotBlank]).
 */
inline val Property<CharSequence>.isNotBlank: Property<Boolean> get() = map(`CharSeqFunc-`.NotBlank)

/**
 * Returns a property representing a trimmed CharSequence ([CharSequence.trim]).
 */
@Suppress("UNCHECKED_CAST")
inline val Property<CharSequence>.trimmed: Property<CharSequence> get() = map(`CharSeqFunc-`.Trim)


//
// common
//

/**
 * Returns a read-only view on this property hiding its original type.
 * Such [Property] cannot be cast to [MutableProperty], which may be used to defend properties
 * exposed via public interface of a module.
 */
@Suppress("UNCHECKED_CAST")
inline fun <T> Property<T>.readOnlyView(): Property<T> = map(Just as (T) -> T)

/**
 * Returns a new [MutableProperty] with value equal to `forward(original.value)`.
 * When mutated, original property receives `backwards(mapped.value)`.
 */
inline fun <T, R> MutableProperty<T>.bind(
        crossinline forward: (T) -> R,
        crossinline backwards: (R) -> T
): MutableProperty<R> =
        `Bound-`<Nothing, T, R>(this, object : `Bound-`.TwoWay<T, R> {
            override fun invoke(p1: T): R = forward.invoke(p1)
            override fun backwards(arg: R): T = backwards.invoke(arg)
        })

/**
 * Returns a new [TransactionalProperty] with value equal to `forward(original.value)`.
 * When mutated, original property receives `backwards(mapped.value)`.
 */
inline fun <TRANSACTION, T, R> TransactionalProperty<TRANSACTION, T>.bind(
        crossinline forward: (T) -> R,
        crossinline backwards: (R) -> T
): TransactionalProperty<TRANSACTION, R> =
        `Bound-`(this, object : `Bound-`.TwoWay<T, R> {
            override fun invoke(p1: T): R = forward.invoke(p1)
            override fun backwards(arg: R): T = backwards.invoke(arg)
        })

/**
 * Returns a property which notifies its subscribers only when old and new values are not equal.
 */
inline fun <T> Property<T>.distinct(noinline areEqual: (T, T) -> Boolean): Property<T> =
        if (this.mayChange) `Distinct-`(this, areEqual) else this

/**
 * Returns a debounced wrapper around this property.
 */
inline fun <T> Property<T>.debounced(delay: Long, unit: TimeUnit = TimeUnit.MILLISECONDS): Property<T> =
        if (mayChange) `Debounced-`(this, delay, unit)
        else this

/**
 * Returns a property of both [this] and [that] values which gets updated when either [this] or [that] gets updated.
 */
@Suppress("UNCHECKED_CAST")
inline fun <T, U> Property<T>.zipWith(that: Property<U>): Property<Pair<T, U>> =
        mapWith(that, ToPair as (T, U) -> Pair<T, U>)

/**
 * Returns a property which has value equal to the value of property returned by `transform(this.value)`.
 * @see flatMapNotNullOrDefault
 */
fun <T, U> Property<T>.flatMap(transform: (T) -> Property<U>): Property<U> =
        `FlatMapped-`(this, transform)


//
// Concurrent
//

/**
 * Inline copy of [java.util.concurrent.atomic.AtomicReference.getAndUpdate].
 */
inline fun <T> MutableProperty<T>.getAndUpdate(updater: (old: T) -> T): T {
    var prev: T
    var next: T
    do {
        prev = value
        next = updater(prev)
    } while (!casValue(prev, next))
    return prev
}

/**
 * Inline copy of [java.util.concurrent.atomic.AtomicReference.updateAndGet].
 */
inline fun <T> MutableProperty<T>.updateAndGet(updater: (old: T) -> T): T {
    var prev: T
    var next: T
    do {
        prev = value
        next = updater(prev)
    } while (!casValue(prev, next))
    return next
}

/**
 * Simplified version of [getAndUpdate] or [updateAndGet].
 */
inline fun <T> MutableProperty<T>.update(updater: (old: T) -> T) {
    var prev: T
    var next: T
    do {
        prev = value
        next = updater(prev)
    } while (!casValue(prev, next))
}


/**
 * Observes this property on [UnconfinedExecutor], i. e. on whatever thread.
 */
inline fun <T> Property<T>.addUnconfinedChangeListener(noinline onChange: ChangeListener<T>) {
    addChangeListenerOn(UnconfinedExecutor, onChange)
}

//
// factories
//

/**
 * Returns new [MutableProperty] with value [value].
 * If [concurrent] is true, the property can be used from many threads.
 */
@Deprecated("Use propertyOf instead.", ReplaceWith("propertyOf(value, concurrent)"))
inline fun <T> mutablePropertyOf(value: T, concurrent: Boolean): MutableProperty<T> =
        if (concurrent) `ConcMutable-`(value)
        else `UnsMutable-`(value)

/**
 * Returns new [MutableProperty] with value [value].
 * Returned property is strictly bounded to current thread.
 */
inline fun <T> propertyOf(value: T): MutableProperty<T> =
        `UnsMutable-`(value)

/**
 * Returns new [MutableProperty] with value [value].
 * When [concurrent] is true, returns a thread-safe property.
 */
inline fun <T> propertyOf(value: T, concurrent: Boolean): MutableProperty<T> =
        if (concurrent) `ConcMutable-`(value)
        else `UnsMutable-`(value)

/**
 * Returns new multi-threaded [MutableProperty] with initial value [value].
 */
@Deprecated("Use concurrentPropertyOf instead.", ReplaceWith("concurrentPropertyOf(value)"))
inline fun <T> concurrentMutablePropertyOf(value: T): MutableProperty<T> =
        `ConcMutable-`(value)

/**
 * Returns new multi-threaded [MutableProperty] with initial value [value].
 */
inline fun <T> concurrentPropertyOf(value: T): MutableProperty<T> =
        `ConcMutable-`(value)

/**
 * Returns new single-threaded [MutableProperty] with initial value [value].
 */
@Deprecated("Use propertyOf instead.", ReplaceWith("propertyOf(value)"))
inline fun <T> unsynchronizedMutablePropertyOf(value: T): MutableProperty<T> =
        `UnsMutable-`(value)

/**
 * Returns an immutable [Property] representing either `true` or `false`.
 */
inline fun immutablePropertyOf(value: Boolean): Property<Boolean> = when (value) {
    true -> TRUE
    false -> FALSE
}

/**
 * Returns an immutable property representing [value].
 */
inline fun <T> immutablePropertyOf(value: T): Property<T> =
        `Immutable-`(value)


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
