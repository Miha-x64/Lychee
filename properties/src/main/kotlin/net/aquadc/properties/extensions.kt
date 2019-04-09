@file:JvmName("Properties")
package net.aquadc.properties

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
 * Returns new property with [transform]ed value.
 * Calling [transform] from [worker] thread is not guaranteed:
 * it will be called in-place if there's no pre-mapped value.
 */
fun <T, R> Property<T>.mapOn(worker: Worker, transform: (T) -> R): Property<R> = when {
    this.mayChange -> `Mapped-`(this, transform, worker)
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
 * If [subscribe], calls [Property.addChangeListener]; if not, calls [Property.removeChangeListener].
 */
fun <T> Property<T>.observeChangesIf(subscribe: Boolean, onChange: ChangeListener<T>) {
    if (subscribe) addChangeListener(onChange)
    else removeChangeListener(onChange)
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
    val v = value
    casValue(v, v)
}
