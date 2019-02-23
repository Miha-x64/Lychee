@file:Suppress("NOTHING_TO_INLINE")
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

/**
 * Every time property becomes non-null,
 * it will be nulled and [action] will be performed.
 */
inline fun <T : Any> MutableProperty<T?>.clearEachAnd(crossinline action: (T) -> Unit): Unit = onEach {
    if (it != null) {
        value = null
        action(it)
    }
}


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
inline fun <T> Property<T>.distinct(noinline dropIfValues: (T, T) -> Boolean): Property<T> =
        if (this.mayChange) `Distinct-`(this, dropIfValues) else this

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
 * Creates a [Property] which receives value of [compute]() every [period].
 */
inline fun <T> updatedEvery(period: Long, unit: TimeUnit = TimeUnit.MILLISECONDS, crossinline compute: () -> T): Property<T> =
        `TimeMapped-`(immutablePropertyOf(Unit), { compute() }, Schedule(period, unit))


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
inline fun <T> concurrentPropertyOf(value: T): MutableProperty<T> =
        `ConcMutable-`(value)

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
