package net.aquadc.properties.internal

import androidx.annotation.RestrictTo
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater

/**
 * This is a [-Notifier] with extra [AtomicReferenceFieldUpdater].
 * @param REF type of extra atomic ref
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
abstract class `Notifier-1AtomicRef`<out T, REF>
constructor(concurrent: Boolean, initialRef: REF)
    : `-Notifier`<T>(concurrent) {

    @Volatile @Suppress("unused")
    protected var ref: REF = initialRef

    @Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
    protected /*inline*/ fun refUpdater() =
            RefUpdater as AtomicReferenceFieldUpdater<`Notifier-1AtomicRef`<@UnsafeVariance T, REF>, REF>

    private companion object {
        private val RefUpdater: AtomicReferenceFieldUpdater<*, *> =
                AtomicReferenceFieldUpdater.newUpdater(`Notifier-1AtomicRef`::class.java, Any::class.java, "ref")
    }

}
