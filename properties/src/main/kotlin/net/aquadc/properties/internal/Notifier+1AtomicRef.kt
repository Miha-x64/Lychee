package net.aquadc.properties.internal

import android.support.annotation.RestrictTo
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater

/**
 * This is a [-Notifier] with extra [AtomicReferenceFieldUpdater].
 * @param REF type of extra atomic ref
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
abstract class `Notifier+1AtomicRef`<out T, REF>
internal constructor(concurrent: Boolean, initialRef: REF)
    : `-Notifier`<T>(concurrent) {

    @Volatile @Suppress("unused")
    protected var ref: REF = initialRef

    @Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
    internal inline fun refUpdater() =
            RefUpdater as AtomicReferenceFieldUpdater<`Notifier+1AtomicRef`<@UnsafeVariance T, REF>, REF>

}
