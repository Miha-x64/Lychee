@file:JvmName("TextViewBindings")
package net.aquadc.properties.android.bindings.widget

import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import android.text.Editable
import android.text.SpannedString
import android.text.TextWatcher
import android.widget.TextView
import net.aquadc.properties.MutableProperty
import net.aquadc.properties.Property
import net.aquadc.properties.android.bindings.Binding
import net.aquadc.properties.android.bindings.bindViewTo
import net.aquadc.properties.android.bindings.bindViewToBinding

/**
 * Binds text to [textProperty] via [TextView.setText].
 */
fun TextView.bindTextTo(textProperty: Property<CharSequence>): Unit =
        bindViewToBinding(textProperty, WatcherAndBinding(this, textProperty, 0)) /* not -1! mind bit flags */

/**
 * Binds text to [textResProperty] via [TextView.setText].
 */
fun TextView.bindTextResTo(textResProperty: Property<Int>): Unit =
        bindViewToBinding(textResProperty, WatcherAndBinding(this, textResProperty, 0))

/**
 * Binds [textProperty] of [CharSequence] to text via [android.text.TextWatcher].
 */
fun TextView.bindToText(textProperty: MutableProperty<in CharSequence>) {
    textProperty.value = SpannedString(text)
    addTextChangedListener(WatcherAndBinding(this, textProperty as Property<Any>, 1))
}

/**
 * Binds [textProperty] of [String] to text via [android.text.TextWatcher].
 */
@JvmName("bindToString")
fun TextView.bindToText(textProperty: MutableProperty<in String>) {
    textProperty.value = text.toString()
    addTextChangedListener(WatcherAndBinding(this, textProperty as Property<Any>, 2))
}

/**
 * Binds [textProperty] with text bidirectionally.
 * When this TextView gets attached to window, text will be set from [textProperty].
 */
fun TextView.bindTextBidirectionally(textProperty: MutableProperty<String>) {
    val watcherAndBinding = WatcherAndBinding(this, textProperty, 2)

    addTextChangedListener(watcherAndBinding)
    bindViewToBinding(textProperty, watcherAndBinding)
}

private class WatcherAndBinding(
        view: TextView,
        property: Property<Any>,
        private var _mode: Int
) : Binding<TextView, Any>(view, property), TextWatcher {

    private val mode get() = _mode and 3
    private var mutatingFromWatcher
        get() = (_mode and 4) != 0
        set(whether) { _mode = if (whether) (_mode or 4) else (_mode and 4.inv()) }
    private var mutatingFromChangeListener
        get() = (_mode and 8) != 0
        set(whether) { _mode = if (whether) (_mode or 8) else (_mode and 8.inv()) }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
        mutatingFromWatcher = true
    }
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }
    override fun afterTextChanged(s: Editable) {
        if (!mutatingFromChangeListener)
            (property as MutableProperty).value = when (mode) {
                1 -> SpannedString(s)
                2 -> s.toString()
                else -> throw AssertionError()
            }
        mutatingFromWatcher = false
    }

    override fun bind(view: TextView, value: Any) {
        // we must check this to avoid resetting selection;
        // note that SpannableStringBuilder calls toString within equals,
        // so we're not going to be slower
        if (view.text?.toString() != value) {
            mutatingFromChangeListener = true
            if (!mutatingFromWatcher) when (value) {
                is CharSequence -> view.text = value
                is Int -> view.setText(value)
                else -> throw AssertionError()
            }
            mutatingFromChangeListener = false
        }
    }
}


/**
 * Binds hint to [hintProperty] via [TextView.setHint].
 */
fun TextView.bindHintTo(hintProperty: Property<CharSequence>): Unit =
        bindViewToBinding(hintProperty, HintBinding(this, hintProperty))

/**
 * Binds hint to [hintResProperty] via [TextView.setHint].
 */
fun TextView.bindHintResTo(hintResProperty: Property<Int>): Unit =
        bindViewToBinding(hintResProperty, HintBinding(this, hintResProperty))

private class HintBinding(
        view: TextView, property: Property<Any>
) : Binding<TextView, Any>(view, property) {
    override fun bind(view: TextView, value: Any) = when (value) {
        is CharSequence -> view.hint = value
        is Int -> view.setHint(value)
        else -> throw AssertionError()
    }
}

/**
 * Binds error message to [errorProperty] via [TextView.setError].
 */
fun TextView.bindErrorMessageTo(errorProperty: Property<CharSequence?>): Unit =
        bindViewToBinding(errorProperty, ErrorBinding(this, errorProperty))

/**
 * Binds error message to [errorResProperty] via [TextView.setError].
 */
fun TextView.bindErrorMessageResTo(errorResProperty: Property<Int>): Unit =
        bindViewToBinding(errorResProperty, ErrorBinding(this, errorResProperty))

/**
 * Binds error message to [errorResProperty] via [TextView.setError].
 */
@Deprecated("renamed", ReplaceWith("this.bindErrorMessageResTo(errorResProperty)", "net.aquadc.properties.android.bindings.widget.bindErrorMessageResTo"))
@JvmName("bindErrorMessageResTo_deprecated")
fun TextView.bindErrorMessageTo(errorResProperty: Property<Int>): Unit =
        bindViewToBinding(errorResProperty, ErrorBinding(this, errorResProperty))

private class ErrorBinding(view: TextView, property: Property<Any?>) : Binding<TextView, Any?>(view, property) {
    override fun bind(view: TextView, value: Any?) = when (value) {
        is CharSequence? -> view.error = value
        is Int -> view.error = (if (value == 0) null else view.resources.getText(value))
        else -> throw AssertionError()
    }
}

/**
 * Binds error message and icon to [errorProperty]
 * via [TextView.setError] (CharSequence, android.graphics.drawable.Drawable).
 */
fun TextView.bindErrorMessageAndIconTo(errorProperty: Property<Pair<CharSequence, Drawable>?>): Unit =
        bindViewToBinding(errorProperty, ErrorWithIconBinding(this, errorProperty))

/**
 * Binds error message and icon to [errorResProperty]
 * via [TextView.setError] (CharSequence, android.graphics.drawable.Drawable).
 */
@JvmName("bindErrorMessageResAndIconTo")
fun TextView.bindErrorMessageAndIconTo(errorResProperty: Property<MessageAndIconRes?>): Unit =
        bindViewTo(errorResProperty, ErrorWithIconBinding(this, errorResProperty))

private class ErrorWithIconBinding(view: TextView, property: Property<Any?>) : Binding<TextView, Any?>(view, property) {

    override fun bind(view: TextView, value: Any?) = when (value) {
        null -> view.setError(null, null)
        is Pair<*, *> -> view.setErrorWithIntrinsicBounds(value.first as CharSequence, value.second as Drawable)
        is MessageAndIconRes -> {
            val res = view.resources
            view.setErrorWithIntrinsicBounds(
                    res.getText(value.messageRes),
                    res.getDrawableCompat(value.iconRes)
            )
        }
        else -> throw AssertionError()
    }

    private fun TextView.setErrorWithIntrinsicBounds(error: CharSequence, icon: Drawable?) {
        icon?.setBounds(0, 0, icon.intrinsicWidth, icon.intrinsicHeight)
        setError(error, icon)
    }
}

internal fun Resources.getDrawableCompat(@DrawableRes id: Int): Drawable? =
        @Suppress("DEPRECATION")
        when {
            id == 0 -> null
            Build.VERSION.SDK_INT >= 21 -> getDrawable(id, null)
            else -> getDrawable(id) // TODO: shouldn't I use ContextCompat, if available?
        }

/**
 * A tuple of two [Int]s for [net.aquadc.properties.android.bindings.widget.bindErrorMessageAndIconTo].
 */
class MessageAndIconRes( // Hey, look at me! I want to be an inline class! Pleeeease!
        @JvmField @StringRes val messageRes: Int,
        @JvmField @DrawableRes val iconRes: Int
)
