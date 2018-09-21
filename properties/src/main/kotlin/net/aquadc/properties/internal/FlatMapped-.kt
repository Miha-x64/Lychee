package net.aquadc.properties.internal

import net.aquadc.properties.ChangeListener
import net.aquadc.properties.Property
import net.aquadc.properties.addUnconfinedChangeListener

@PublishedApi
internal class `FlatMapped-`<in T, out U>(
        @JvmField internal val original: Property<@UnsafeVariance T>,
        @JvmField internal val map: (T) -> Property<U>
) : `Notifier-1AtomicRef`<U, @UnsafeVariance U>(original.isConcurrent, unset()), ChangeListener<@UnsafeVariance U> {

    init {
        check(original.mayChange)
    }

    @Volatile @JvmField
    internal var master: Property<@UnsafeVariance U>? = null

    private val originalChanged: ChangeListener<T> = { _, new ->
        if (isBeingObserved()) {
            if (thread == null) withLockedTransition { processOriginalChange(new) }
            else processOriginalChange(new)
        }
    }

    internal fun processOriginalChange(new: T) {
        master!!.removeChangeListener(this)
        val newMaster = map(new)
        master = newMaster
        invoke(ref, newMaster.value)
        newMaster.addUnconfinedChangeListener(this)
    }

    // master changed
    override fun invoke(old: @UnsafeVariance U, new: @UnsafeVariance U) {
        ref = new
        valueChanged(old, new, null)
    }

    override fun observedStateChanged(observed: Boolean) {
        if (observed) {
            val newMaster = map(original.value)
            master = newMaster
            refUpdater().eagerOrLazySet(this, thread, newMaster.value)
            original.addUnconfinedChangeListener(originalChanged)
            newMaster.addUnconfinedChangeListener(this)
        } else {
            master!!.removeChangeListener(this)
            original.removeChangeListener(originalChanged)
            refUpdater().eagerOrLazySet(this, thread, unset())
        }
    }

    override val value: U
        get() {
            if (thread !== null) checkThread()
            val value = ref

            // if not observed, calculate on demand
            return if (value === Unset) map(original.value).value else value
        }

}
