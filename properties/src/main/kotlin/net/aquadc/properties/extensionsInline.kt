@file:Suppress("NOTHING_TO_INLINE", "DeprecatedCallableAddReplaceWith")
@file:JvmName("PropertiesInline")
package net.aquadc.properties

import net.aquadc.properties.executor.UnconfinedExecutor
import net.aquadc.properties.function.*
import net.aquadc.properties.internal.*
import java.util.concurrent.TimeUnit


/**
 * Observer for [Property]<T>.
 */
typealias ChangeListener<T> = (old: T, new: T) -> Unit
// typealias just takes place in metadata, let it live with inline functions


//
// T(s) -> Boolean
//

/**
 * Returns a view on [this] == [that].
 */
@Deprecated("Was generalized.", ReplaceWith("mapWith(that, areEqual())",
        "net.aquadc.properties.mapWith", "net.aquadc.properties.function.areEqual"))
inline fun <T> Property<T>.equalTo(that: Property<T>): Property<Boolean> =
        mapWith(that, areEqual())

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
@Deprecated("Was generalized.", ReplaceWith("map(length())",
        "net.aquadc.properties.map", "net.aquadc.properties.function.length"))
inline val Property<CharSequence>.length: Property<Int> get() = map(length())

/**
 * Returns a property representing emptiness ([CharSequence.isEmpty]).
 */
@Deprecated("Was generalized.", ReplaceWith("map(isEmptyCharSequence())",
        "net.aquadc.properties.map", "net.aquadc.properties.function.isEmptyCharSequence"))
inline val Property<CharSequence>.isEmpty: Property<Boolean> get() = map(isEmptyCharSequence())

/**
 * Returns a property representing non-emptiness ([CharSequence.isNotEmpty]).
 */
@Deprecated("Was generalized.", ReplaceWith("map(isNonEmptyCharSequence())",
        "net.aquadc.properties.map", "net.aquadc.properties.function.isNonEmptyCharSequence"))
inline val Property<CharSequence>.isNotEmpty: Property<Boolean> get() = map(isNonEmptyCharSequence())

/**
 * Returns a property representing blankness ([CharSequence.isBlank]).
 */
@Deprecated("Was generalized.", ReplaceWith("map(isBlank())",
        "net.aquadc.properties.map", "net.aquadc.properties.function.isBlank"))
inline val Property<CharSequence>.isBlank: Property<Boolean> get() = map(isBlank())

/**
 * Returns a property representing non-blankness ([CharSequence.isNotBlank]).
 */
@Deprecated("Was generalized.", ReplaceWith("map(isNotBlank())",
        "net.aquadc.properties.map", "net.aquadc.properties.function.isNotBlank"))
inline val Property<CharSequence>.isNotBlank: Property<Boolean> get() = map(isNotBlank())

/**
 * Returns a property representing a trimmed CharSequence ([CharSequence.trim]).
 */
@Deprecated("Was generalized.", ReplaceWith("map(trimmed())",
        "net.aquadc.properties.map", "net.aquadc.properties.function.trimmed"))
inline val Property<CharSequence>.trimmed: Property<CharSequence> get() = map(trimmed())


//
// common
//

/**
 * Returns a read-only view on this property hiding its original type.
 * Such [Property] cannot be cast to [MutableProperty], which may be used to defend properties
 * exposed via public interface of a module.
 */
@Suppress("UNCHECKED_CAST")
inline fun <T> Property<T>.readOnlyView(): Property<T> = map(identity())

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
inline fun <T, U> Property<T>.flatMap(noinline transform: (T) -> Property<U>): Property<U> =
        `FlatMapped-`(this, transform)

/**
 * Creates a [Property] which receives value of [compute]() every [delay].
 */
inline fun <T> updatedEvery(delay: Long, unit: TimeUnit = TimeUnit.MILLISECONDS, crossinline compute: () -> T): Property<T> =
        `TimeMapped-`(immutablePropertyOf(Unit), { compute() }, Schedule(delay, unit))


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
@Deprecated("Use propertyOf instead.", ReplaceWith("propertyOf(value, concurrent)"), DeprecationLevel.ERROR)
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
@Deprecated("Use concurrentPropertyOf instead.", ReplaceWith("concurrentPropertyOf(value)"), DeprecationLevel.ERROR)
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
 * Returns an immutable [Property] with [Unit] value.
 */
inline fun immutablePropertyOf(@Suppress("UNUSED_PARAMETER") value: Unit): Property<Unit> =
        UNIT

/**
 * Returns an immutable property representing [value].
 */
inline fun <T> immutablePropertyOf(value: T): Property<T> =
        `Immutable-`(value)

/**
 * Helps finding out necessary functions.
 * @see isEmptyCharSequence
 * @see isEmptyCollection
 */
@Deprecated("Use either isEmptyCharSequence() or isEmptyCollection().", level = DeprecationLevel.ERROR)
inline fun isEmpty(): Nothing =
        throw AssertionError()


/**
 * Helps finding out necessary functions.
 * @see isNonEmptyCharSequence
 * @see isNonEmptyCollection
 */
@Deprecated("Use either isNonEmptyCharSequence() or isNonEmptyCollection().", level = DeprecationLevel.ERROR)
inline fun isNotEmpty(): Nothing =
        throw AssertionError()
