@file:JvmName("Properties")
package net.aquadc.properties

import net.aquadc.properties.diff.DiffChangeListener
import net.aquadc.properties.diff.DiffProperty
import net.aquadc.properties.executor.InPlaceWorker
import net.aquadc.properties.executor.Worker
import net.aquadc.properties.function.OnEach
import net.aquadc.properties.internal.`BiMapped-`
import net.aquadc.properties.internal.`Immutable-`
import net.aquadc.properties.internal.`Mapped-`

/**
 * Returns new property with [transform]ed value which depends on [this] property value.
 */
fun <T, R> Property<T>.map(transform: (T) -> R): Property<R> = when {
    this.mayChange -> `Mapped-`(this, transform, InPlaceWorker)
    else -> immutablePropertyOf(transform(value))
}

/**
 * Returns new property with [transform]ed value depending on two properties' values.
 */
fun <T, U, R> Property<T>.mapWith(that: Property<U>, transform: (T, U) -> R): Property<R> = when {
    this.mayChange || that.mayChange ->
        `BiMapped-`(this, that, transform)

    else ->
        `Immutable-`(transform(this.value, that.value))
}



/**
 * Calls [func] for each [Property.value] including initial.
 */
fun <P : Property<T>, T> P.onEach(func: (T) -> Unit): P = apply { // TODO: concurrent correctness
    if (isConcurrent) {
        val proxy = object : OnEach<T>() {

            override fun invoke(p1: T) =
                    func(p1)

        }
        addChangeListener(proxy)

        /*
          In the worst case scenario, change will happen in parallel just after subscription,
          invoke(T, T) will start running;
          we will CAS successfully and func will run in parallel.
        */

        // if calledRef is still not null
        // and has value of 'false',
        // then our function was not called yet.
        if (proxy.compareAndSet(false, true)) {
            func(value) // run function, ASAP!
        } // else we have more fresh value, don't call func
    } else {
        addChangeListener { _, new -> func(new) }
        func(value)
    }
}

/**
 * If [subscribe], adds [onChange] as a listener to this property and invokes it with up to date value;
 * if not, unsubscribes and invokes [onChange] with [dummy] value.
 *
 * This could be used with ObservingAdapter to avoid listening changes when RecyclerView is detached.
 *
 * [onChange] value must be stable between invocations.
 * Avoid passing a lambda, anonymous fun, object declaration, or bound function reference
 * in place, store it in a field. (Yep, [onChange] parameter is intentionally not last.)
 */
@JvmName("syncUsingDummyIf")
fun <T> Property<T>.syncIf(subscribe: Boolean, onChange: ChangeListener<T>, dummy: T, _hack: Unit? = null) {
    if (subscribe) {
        addChangeListener(onChange)
        onChange(dummy, value)
    } else {
        removeChangeListener(onChange)
        onChange(value, dummy)
    }
}

@JvmName("syncUsingDummyIf")
fun <T, D : Any> DiffProperty<T, D>.syncIf(subscribe: Boolean, onChange: DiffChangeListener<T, D?>, dummy: T, _hack: Unit? = null) {
    if (subscribe) {
        addChangeListener(onChange)
        onChange(dummy, value, null)
    } else {
        removeChangeListener(onChange)
        onChange(value, dummy, null)
    }
}

/**
 * If [subscribe], adds [onChange] as a listener to this property and invokes it with up to date value;
 * if not, unsubscribes.
 *
 * This could be used with ObservingAdapter to avoid listening changes when RecyclerView is detached.
 * Pass the value left from the last change using [stale] parameter.
 *
 * [onChange] value must be stable between invocations.
 * Avoid passing a lambda, anonymous fun, object declaration, or bound function reference
 * in place, store it in a field. (Yep, [onChange] parameter is intentionally not last.)
 */
@JvmName("syncUsingStaleIf")
fun <T> Property<T>.syncIf(subscribe: Boolean, onChange: ChangeListener<T>, stale: T, _hack: Nothing? = null) {
    if (subscribe) {
        addChangeListener(onChange)
        onChange(stale, value)
    } else {
        removeChangeListener(onChange)
    }
}

@JvmName("syncUsingStaleIf")
fun <T, D : Any> DiffProperty<T, D>.syncIf(subscribe: Boolean, onChange: DiffChangeListener<T, D?>, stale: T, _hack: Nothing? = null) {
    if (subscribe) {
        addChangeListener(onChange)
        onChange(stale, value, null)
    } else {
        removeChangeListener(onChange)
    }
}

/**
 * Returns a property which has value equal to the value of property returned by `transform(this.value)` when
 * [this] has not `null` value, or [default] otherwise.
 * @see flatMap
 */
fun <T : Any, U> Property<T?>.flatMapNotNullOrDefault(default: U, transform: (T) -> Property<U>): Property<U> {
    val fallbackProp = immutablePropertyOf(default)
    return flatMap { if (it == null) fallbackProp else transform(it) }
}

/**
 * Drops current binding, if any.
 */
fun <T> MutableProperty<T>.unbind() {
    do {
        val v = value
    } while (!casValue(v, v))
}

/**
 * Sets [this] value to its compliment.
 */
fun MutableProperty<Boolean>.flip() {
    do {
        val v = value
    } while (!casValue(v, !v))
}
