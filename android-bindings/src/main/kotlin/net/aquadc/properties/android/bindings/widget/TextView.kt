package net.aquadc.properties.android.bindings.widget

import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Build
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import android.text.Editable
import android.text.SpannedString
import android.widget.TextView
import net.aquadc.properties.MutableProperty
import net.aquadc.properties.Property
import net.aquadc.properties.android.bindings.bindViewTo
import net.aquadc.properties.android.simple.SimpleTextWatcher

/**
 * Binds text to [textProperty] via [TextView.setText].
 */
fun TextView.bindTextTo(textProperty: Property<CharSequence>) =
        bindViewTo(textProperty, SetText)

/**
 * Binds text to [textResProperty] via [TextView.setText].
 */
@JvmName("bindTextResTo")
fun TextView.bindTextTo(textResProperty: Property<Int>) =
        bindViewTo(textResProperty, SetText)

private object SetText : (TextView, Any) -> Unit {
    override fun invoke(p1: TextView, p2: Any) = when (p2) {
        is CharSequence -> p1.text = p2
        is Int -> p1.setText(p2)
        else -> throw AssertionError()
    }
}

/**
 * Binds [textProperty] to text via [android.text.TextWatcher].
 */
fun TextView.bindToText(textProperty: MutableProperty<CharSequence>) {
    textProperty.value = text.toString()
    addTextChangedListener(object : SimpleTextWatcher() {
        override fun afterTextChanged(s: Editable) {
            textProperty.value = SpannedString(s)
        }
    })
}

/**
 * Binds [textProperty] with text bidirectionally.
 * When this TextView gets attached to window, text will be set from [textProperty].
 */
fun TextView.bindTextBidirectionally(textProperty: MutableProperty<String>) {
    val watcherAndBinding = object : SimpleTextWatcher(), (TextView, String) -> Unit {

        private var mutatingFromWatcher = false
        private var mutatingFromChangeListener = false

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            mutatingFromWatcher = true
        }
        override fun afterTextChanged(s: Editable) {
            if (!mutatingFromChangeListener) textProperty.value = s.toString()
            mutatingFromWatcher = false
        }

        override fun invoke(p1: TextView, p2: String) {
            mutatingFromChangeListener = true
            if (!mutatingFromWatcher) p1.text = p2
            mutatingFromChangeListener = false
        }
    }

    addTextChangedListener(watcherAndBinding)
    bindViewTo(textProperty, watcherAndBinding)
}


/**
 * Binds hint to [hintProperty] via [TextView.setHint].
 */
fun TextView.bindHintTo(hintProperty: Property<CharSequence>) =
        bindViewTo(hintProperty, SetHint)

/**
 * Binds hint to [hintProperty] via [TextView.setHint].
 */
@JvmName("bindHintResTo")
fun TextView.bindHintTo(hintResProperty: Property<Int>) =
        bindViewTo(hintResProperty, SetHint)

private object SetHint : (TextView, Any) -> Unit {
    override fun invoke(p1: TextView, p2: Any) = when (p2) {
        is CharSequence -> p1.hint = p2
        is Int -> p1.setHint(p2)
        else -> throw AssertionError()
    }
}

/**
 * Binds error message to [errorProperty] via [TextView.setError].
 */
fun TextView.bindErrorMessageTo(errorProperty: Property<CharSequence?>) =
        bindViewTo(errorProperty, SetError)

/**
 * Binds error message to [errorResProperty] via [TextView.setError].
 */
@JvmName("bindErrorMessageResTo")
fun TextView.bindErrorMessageTo(errorResProperty: Property<Int>) =
        bindViewTo(errorResProperty, SetError)

private object SetError : (TextView, Any?) -> Unit {
    override fun invoke(p1: TextView, p2: Any?) = when (p2) {
        is CharSequence? -> p1.error = p2
        is Int -> p1.error = (if (p2 == 0) null else p1.resources.getText(p2))
        else -> throw AssertionError()
    }
}

/**
 * Binds error message and icon to [errorProperty]
 * via [TextView.setError] (CharSequence, android.graphics.drawable.Drawable).
 */
fun TextView.bindErrorMessageAndIconTo(errorProperty: Property<Pair<CharSequence, Drawable>?>) =
        bindViewTo(errorProperty, BindErrorMessageAndIconTo)

/**
 * Binds error message and icon to [errorResProperty]
 * via [TextView.setError] (CharSequence, android.graphics.drawable.Drawable).
 */
@JvmName("bindErrorMessageResAndIconTo")
fun TextView.bindErrorMessageAndIconTo(errorResProperty: Property<MessageAndIconRes?>) =
        bindViewTo(errorResProperty, BindErrorMessageAndIconTo)

private object BindErrorMessageAndIconTo : (TextView, Any?) -> Unit {

    override fun invoke(v: TextView, new: Any?) = when (new) {
        null -> v.setError(null, null)
        is Pair<*, *> -> v.setErrorWithIntrinsicBounds(new.first as CharSequence, new.second as Drawable)
        is MessageAndIconRes -> {
            val res = v.resources
            v.setErrorWithIntrinsicBounds(
                    res.getText(new.messageRes),
                    res.getDrawableCompat(new.iconRes)
            )
        }
        else -> throw AssertionError()
    }

}

internal fun Resources.getDrawableCompat(@DrawableRes id: Int): Drawable? =
        @Suppress("DEPRECATION")
        when {
            id == 0 -> null
            Build.VERSION.SDK_INT >= 21 -> getDrawable(id, null)
            else -> getDrawable(id)
        }

/**
 * A tuple of two [Int]s for [net.aquadc.properties.android.bindings.bindErrorMessageAndIconTo].
 */
class MessageAndIconRes(
        @JvmField @StringRes val messageRes: Int,
        @JvmField @DrawableRes val iconRes: Int
)

private fun TextView.setErrorWithIntrinsicBounds(error: CharSequence, icon: Drawable?) {
    icon?.setBounds(0, 0, icon.intrinsicWidth, icon.intrinsicHeight)
    setError(error, icon)
}
