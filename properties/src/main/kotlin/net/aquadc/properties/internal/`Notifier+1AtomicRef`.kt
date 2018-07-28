package net.aquadc.properties.internal

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater

/**
 * This is a [-Notifier] with extra [AtomicReferenceFieldUpdater].
 * @param REF type of extra atomic ref
 */
abstract class `Notifier+1AtomicRef`<out T, REF>
internal constructor(concurrent: Boolean, initialRef: REF)
    : `-Notifier`<T>(concurrent) {

    @Volatile @Suppress("unused")
    protected var ref: REF = initialRef

    internal companion object {
        @JvmField internal val RefUpdater: AtomicReferenceFieldUpdater<*, *> =
                AtomicReferenceFieldUpdater.newUpdater(`Notifier+1AtomicRef`::class.java, Any::class.java, "ref")

        @Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
        internal inline fun <T, REF> `Notifier+1AtomicRef`<out T, REF>.refUpdater() =
                RefUpdater as AtomicReferenceFieldUpdater<`Notifier+1AtomicRef`<out T, REF>, REF>
    }

}
