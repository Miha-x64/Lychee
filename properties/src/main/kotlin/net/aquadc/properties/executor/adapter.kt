package net.aquadc.properties.executor

import net.aquadc.properties.ChangeListener
import net.aquadc.properties.diff.DiffChangeListener
import net.aquadc.properties.internal.Unset
import net.aquadc.properties.internal.unset
import java.lang.UnsupportedOperationException
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicInteger

internal class MapWhenChanged<in T, U>(
        private val mapOn: Worker,
        private val map: (T) -> U,
        private val consumer: (U) -> Unit
) : ChangeListener<T> {

    override fun invoke(old: T, new: T) {
        mapOn.map(new, map, consumer)
    }

}

internal class ConsumeOn<in T>(
        private val consumeOn: Executor,
        private val consumer: (T) -> Unit
) : (T) -> Unit, Runnable {

    @Volatile
    private var value: T = unset()

    override fun invoke(value: T) {
        check(value !== Unset)
        this.value = value
        consumeOn.execute(this)
    }

    override fun run() {
        val value = value
        check(value !== this)
        consumer(value)
    }

}

/**
 * When invoked, calls [actual] on [executor].
 */
internal class ConfinedChangeListener<in T, in D>(
        private val executor: Executor,
        @JvmField internal val actual: ChangeListener<T>?,
        @JvmField internal val actualDiff: DiffChangeListener<T, D>?
) : AtomicInteger(0), ChangeListener<T>, DiffChangeListener<T, D> {

    @Volatile @JvmField
    internal var canceled = false

    init {
        check(actual !is ConfinedChangeListener<*, *>)
        check(actualDiff !is ConfinedChangeListener<*, *>)
    }

    @Suppress("UNCHECKED_CAST") // it's safe because `actualDiff` listener is null in this case
    override fun invoke(old: T, new: T) {
        invoke(old, new, null as D)
    }

    override fun invoke(old: T, new: T, diff: D) {
        if (PlatformExecutors.executors.get() === executor && get() == 0) {
            // call listener in-place, copying single-threaded properties' behaviour and creating less overhead
            // AtomicInteger keeps track of pending notifications. Can notify in-place only if there are 0 of them
            // TODO: write a test on this behaviour

            if (actual !== null) actual.invoke(old, new)
            else actualDiff!!.invoke(old, new, diff)
        } else {
            incrementAndGet()
            executor.execute {
                decrementAndGet()
                if (!canceled) {
                    if (actual !== null) actual.invoke(old, new)
                    else actualDiff!!.invoke(old, new, diff)
                }
            }
        }
    }

    // https://youtrack.jetbrains.com/issue/KT-16087

    override fun toByte(): Byte =
            throw UnsupportedOperationException()

    override fun toChar(): Char =
            throw UnsupportedOperationException()

    override fun toShort(): Short =
            throw UnsupportedOperationException()

}

/**
 * Executes given command in-place.
 */
object UnconfinedExecutor : Executor {

    override fun execute(command: Runnable): Unit =
            command.run()

}
