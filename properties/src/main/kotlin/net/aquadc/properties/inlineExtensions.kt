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
@Suppress("UNCHECKED_CAST")
inline fun <T> Property<T>.equalTo(that: Property<T>): Property<Boolean> =
        mapWith(that, `ToBoolFunc-` as (T, T) -> Boolean)


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
// bulk
//


/**
 * Maps a list of properties into a single [Property] by transforming a [List] of their values.
 */
inline fun <T, R> Collection<Property<T>>.mapValueList(noinline transform: (List<T>) -> R): Property<R> =
        `MultiMapped-`(this, transform)

/**
 * Folds a list of properties into a single [Property].
 */
@Suppress("UNCHECKED_CAST")
inline fun <T, R> Collection<Property<T>>.foldValues(initial: R, crossinline operation: (acc: R, T) -> R): Property<R> =
        mapValueList({ it: Any ->
            it as List<T>

            var accumulator = initial
            for (element in it) accumulator = operation(accumulator, element)
            val ret: Any? = accumulator
            ret
        } as (List<T>) -> R)

/**
 * Returns a property which holds first non-null property's value, or null, if all are nulls.
 */
@Suppress("UNCHECKED_CAST")
inline fun <T> Collection<Property<T>>.firstValueOrNull(crossinline predicate: (T) -> Boolean): Property<T?> =
        mapValueList({ it: Any ->
            it as List<T>

            val ret: Any? = it.firstOrNull(predicate)
            ret
        } as (List<T>) -> T?)

/**
 * Returns a property which holds a filtered collection.
 */
@Suppress("UNCHECKED_CAST")
inline fun <T> Collection<Property<T>>.filterValues(crossinline predicate: (T) -> Boolean): Property<List<T>> =
        mapValueList({ it: Any ->
            it as List<T>

            val ret: Any? = it.filter(predicate)
            ret
        } as (List<T>) -> List<T>)

/**
 * Returns a property which holds `true` if [this] contains a property holding [value].
 */
@Suppress("UNCHECKED_CAST")
inline fun <T> Collection<Property<T>>.containsValue(value: T): Property<Boolean> =
        mapValueList(`Contains-`<T>(value, false) as (List<T>) -> Boolean)

/**
 * Returns a property which holds `true` if [this] contains a property holding all [values].
 */
@Suppress("UNCHECKED_CAST")
inline fun <T> Collection<Property<T>>.containsAllValues(values: Collection<T>): Property<Boolean> =
        mapValueList(`Contains-`<T>(values, true) as (List<T>) -> Boolean)

/**
 * Returns a property which holds `true` if all properties' values in [this] are conforming to [predicate].
 */
@Suppress("UNCHECKED_CAST")
inline fun <T> Collection<Property<T>>.allValues(crossinline predicate: (T) -> Boolean): Property<Boolean> =
        mapValueList({ it: Any ->
            it as List<T>

            val ret: Any? = it.all(predicate)
            ret
        } as (List<T>) -> Boolean)

/**
 * Returns a property which holds `true` if any of properties' values in [this] conforms to [predicate].
 */
@Suppress("UNCHECKED_CAST")
inline fun <T> Collection<Property<T>>.anyValue(crossinline predicate: (T) -> Boolean): Property<Boolean> =
        mapValueList({ it: Any ->
            @Suppress("UNCHECKED_CAST")
            it as List<T>

            val ret: Any? = it.any(predicate)
            ret
        } as (List<T>) -> Boolean)


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
