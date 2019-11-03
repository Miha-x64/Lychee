@file:JvmName("TextViewBindings")
@file:Suppress("NOTHING_TO_INLINE")
package net.aquadc.properties.android.bindings.widget

import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
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
@Suppress("UNCHECKED_CAST")
inline fun TextView.bindTextTo(textProperty: Property<CharSequence>): Unit =
        bindViewTo(textProperty, SetText as (TextView, CharSequence) -> Unit)

/**
 * Binds text to [textResProperty] via [TextView.setText].
 */
@Deprecated("Renamed", ReplaceWith("this.bindTextResTo(textResProperty)", "net.aquadc.properties.android.bindings.widget.bindTextResTo"))
@JvmName("bindTextResTo_deprecated") @Suppress("UNCHECKED_CAST")
inline fun TextView.bindTextTo(textResProperty: Property<Int>): Unit =
        bindViewTo(textResProperty, SetText as (TextView, Int) -> Unit)

/**
 * Binds text to [textResProperty] via [TextView.setText].
 */
@Suppress("UNCHECKED_CAST")
inline fun TextView.bindTextResTo(textResProperty: Property<Int>): Unit =
        bindViewTo(textResProperty, SetText as (TextView, Int) -> Unit)

@PublishedApi internal object SetText : (Any?, Any?) -> Any? {
    override fun invoke(p1: Any?, p2: Any?): Any? {
        p1 as TextView
        when (p2) {
            is CharSequence -> if (p1.text?.toString() != p2) p1.text = p2
            is Int -> p1.setText(p2)
            else -> throw AssertionError()
        }
        return Unit
    }
}

/**
 * Binds [textProperty] of [CharSequence] to text via [android.text.TextWatcher].
 */
fun TextView.bindToText(textProperty: MutableProperty<in CharSequence>) {
    textProperty.value = SpannedString(text)
    addTextChangedListener(PropertySettingWatcher(1, textProperty))
}

/**
 * Binds [textProperty] of [String] to text via [android.text.TextWatcher].
 */
@JvmName("bindToString")
fun TextView.bindToText(textProperty: MutableProperty<in String>) {
    textProperty.value = text.toString()
    @Suppress("UNCHECKED_CAST")
    addTextChangedListener(PropertySettingWatcher(2, textProperty as MutableProperty<in CharSequence>))
}


private class PropertySettingWatcher(
        private val mode: Int,
        private val property: MutableProperty<in CharSequence>
) : SimpleTextWatcher() {

    override fun afterTextChanged(s: Editable) {
        property.value = when (mode) {
            1 -> SpannedString(s)
            2 -> s.toString()
            else -> throw AssertionError()
        }
    }

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
            // we must check this to avoid resetting selection;
            // note that SpannableStringBuilder calls toString within equals,
            // so we're not going to be slower
            if (p1.text?.toString() != p2) {
                mutatingFromChangeListener = true
                if (!mutatingFromWatcher) p1.text = p2
                mutatingFromChangeListener = false
            }
        }
    }

    addTextChangedListener(watcherAndBinding)
    bindViewTo(textProperty, watcherAndBinding)
}


/**
 * Binds hint to [hintProperty] via [TextView.setHint].
 */
inline fun TextView.bindHintTo(hintProperty: Property<CharSequence>): Unit =
        bindViewTo(hintProperty, SetHint)

/**
 * Binds hint to [hintResProperty] via [TextView.setHint].
 */
@JvmName("bindHintResTo_deprecated")
@Deprecated("Renamed", ReplaceWith("this.bindHintResTo(hintResProperty)", "net.aquadc.properties.android.bindings.widget.bindHintResTo"))
inline fun TextView.bindHintTo(hintResProperty: Property<Int>): Unit =
        bindViewTo(hintResProperty, SetHint)

/**
 * Binds hint to [hintResProperty] via [TextView.setHint].
 */
inline fun TextView.bindHintResTo(hintResProperty: Property<Int>): Unit =
        bindViewTo(hintResProperty, SetHint)

@PublishedApi internal object SetHint : (TextView, Any) -> Unit {
    override fun invoke(p1: TextView, p2: Any) = when (p2) {
        is CharSequence -> p1.hint = p2
        is Int -> p1.setHint(p2)
        else -> throw AssertionError()
    }
}

/**
 * Binds error message to [errorProperty] via [TextView.setError].
 */
inline fun TextView.bindErrorMessageTo(errorProperty: Property<CharSequence?>): Unit =
        bindViewTo(errorProperty, SetError)

/**
 * Binds error message to [errorResProperty] via [TextView.setError].
 */
@JvmName("bindErrorMessageResTo")
inline fun TextView.bindErrorMessageTo(errorResProperty: Property<Int>): Unit =
        bindViewTo(errorResProperty, SetError)

@PublishedApi internal object SetError : (TextView, Any?) -> Unit {
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
inline fun TextView.bindErrorMessageAndIconTo(errorProperty: Property<Pair<CharSequence, Drawable>?>): Unit =
        bindViewTo(errorProperty, BindErrorMessageAndIconTo)

/**
 * Binds error message and icon to [errorResProperty]
 * via [TextView.setError] (CharSequence, android.graphics.drawable.Drawable).
 */
@JvmName("bindErrorMessageResAndIconTo")
inline fun TextView.bindErrorMessageAndIconTo(errorResProperty: Property<MessageAndIconRes?>): Unit =
        bindViewTo(errorResProperty, BindErrorMessageAndIconTo)

@PublishedApi internal object BindErrorMessageAndIconTo : (TextView, Any?) -> Unit {

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
            else -> getDrawable(id) // TODO: shouldn't I use ContextCompat, if available?
        }

/**
 * A tuple of two [Int]s for [net.aquadc.properties.android.bindings.widget.bindErrorMessageAndIconTo].
 */
class MessageAndIconRes(
        @JvmField @StringRes val messageRes: Int,
        @JvmField @DrawableRes val iconRes: Int
)

internal fun TextView.setErrorWithIntrinsicBounds(error: CharSequence, icon: Drawable?) {
    icon?.setBounds(0, 0, icon.intrinsicWidth, icon.intrinsicHeight)
    setError(error, icon)
}
