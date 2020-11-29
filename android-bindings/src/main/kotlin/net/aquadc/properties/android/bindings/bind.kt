@file:JvmName("Bindings")
package net.aquadc.properties.android.bindings

import android.os.Build
import android.os.Looper
import android.view.View
import androidx.annotation.RequiresApi
import androidx.annotation.UiThread
import net.aquadc.properties.Property
import net.aquadc.properties.addUnconfinedChangeListener
//import net.aquadc.properties.android.observeStartedIf


/**
 * Binds view to [source] property via [bind] function.
 *
 * For properties which [Property.mayChange],
 * calls [bind] for each [source] change while [View.isAttachedToWindow],
 * and on [View.OnAttachStateChangeListener.onViewAttachedToWindow].
 *
 * For immutable properties, calls [bind] in-place.
 *
 * Note: caller (inliner) of this function declares an anonymous class.
 */
inline fun <V : View, T> V.bindViewTo(source: Property<T>, crossinline bind: (view: V, new: T) -> Unit) {
    if (source.mayChange) {
        attach(
                object : Binding<V, T>(this, source) {
                    override fun bind(view: V, value: T) =
                            bind.invoke(view, value)
                }
        )
    } else {
        bind.invoke(this, source.value)
    }
}

// the best option when you need to declare and instantiate your own anonymous class
internal fun <V : View, T> V.bindViewToBinding(source: Property<T>, binding: Binding<V, T>) {
    if (source.mayChange) attach(binding)
    else binding.bind(this, source.value)
}

@PublishedApi internal fun <T, V : View> V.attach(binding: Binding<V, T>) {
    // the order is important: binding code from onViewAttachedToWindow() could remove view (like LazyView does)
    // and we must get onViewDetachedFromWindow() called.
    addOnAttachStateChangeListener(binding)

    // if the view is already attached, catch up with this state
    if (windowToken != null) binding.onViewAttachedToWindow(this)
}

/**
 * Binds view [destination] property to [source] property.
 *
 * For properties which [Property.mayChange],
 * [android.util.Property.set]s new value for each [source] change while [View.isAttachedToWindow],
 * and on [View.OnAttachStateChangeListener.onViewAttachedToWindow].
 *
 * For immutable properties, calls [android.util.Property.set] in-place.
 */
@RequiresApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
fun <V : View, T> V.bindViewTo(source: Property<T>, destination: android.util.Property<V, T>): Unit =
        bindViewTo(source) { obj, value -> destination.set(obj, value) }


@PublishedApi internal abstract class Binding<V : View, in T>(
        @JvmField protected val view: V,
        @JvmField protected val property: Property<@UnsafeVariance T>
) : View.OnAttachStateChangeListener
//      , (Boolean) -> Unit // started state changed
      , (T, T) -> Unit, Runnable // value changed
{

    @UiThread final override fun onViewAttachedToWindow(v: View) {
        /*view.context.observeStartedIf(true, this)*/ invoke(true)
    }
    @UiThread final override fun onViewDetachedFromWindow(v: View) {
        /*view.context.observeStartedIf(false, this)*/ invoke(false)
    }

    @UiThread /*override*/ fun invoke(isStarted: Boolean) { // View is attached, let's think Activity is started
        if (isStarted) {
            // We're probably the first listener, thus, our subscription will trigger value computation.
            property.addUnconfinedChangeListener(this) // run() will call view.post() from any BG thread

            run() // Now let's peek value, it is already computed at this point.
        } else {
            property.removeChangeListener(this)
            view.removeCallbacks(this) // we could be posted
        }
    }

    /*@AnyThread*/ final override fun invoke(p1: T, p2: T) { // View is attached
        if (Looper.myLooper() == view.handler.looper) {
            run() // Lint can't infer UiThread in this block, that's why @AnyThread is commented out.
        } else { // post() can lead to double-triple-whatever posts, even from different threads,
            view.post(this) // but run() gonna debounce 'em all.
        }
    }

    @UiThread final override fun run() {
        view.removeCallbacks(this) // debounce, whatever the thread
        bind(view, property.value)
    }

    abstract fun bind(view: V, value: T)

}

// Note: README.md contains links to this file with line numbers of bindViewTo(Property, function) and Binding.
