package net.aquadc.properties.android.bindings

import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.SpannedString
import android.widget.TextView
import net.aquadc.properties.MutableProperty
import net.aquadc.properties.Property
import net.aquadc.properties.android.compat.getDrawableCompat
import net.aquadc.properties.android.container.MessageAndIconRes
import net.aquadc.properties.android.extension.getTextOrNullIfZero
import net.aquadc.properties.android.extension.setErrorWithIntrinsicBounds
import net.aquadc.properties.android.simple.SimpleTextWatcher


fun TextView.bindTextTo(textProperty: Property<CharSequence>) =
        bindViewTo(textProperty, SetText)

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

fun TextView.bindToText(textProperty: MutableProperty<CharSequence>) {
    textProperty.value = text.toString()
    addTextChangedListener(object : SimpleTextWatcher() {
        override fun afterTextChanged(s: Editable) {
            textProperty.value = SpannedString(s)
        }
    })
}

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


fun TextView.bindHintTo(hintProperty: Property<CharSequence>) =
        bindViewTo(hintProperty, SetHint)

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

fun TextView.bindErrorMessageTo(errorProperty: Property<CharSequence?>) =
        bindViewTo(errorProperty, SetError)

@JvmName("bindErrorMessageResTo")
fun TextView.bindErrorMessageTo(errorResProperty: Property<Int>) =
        bindViewTo(errorResProperty, SetError)

private object SetError : (TextView, Any?) -> Unit {
    override fun invoke(p1: TextView, p2: Any?) = when (p2) {
        is CharSequence? -> p1.error = p2
        is Int -> p1.error = p1.resources.getTextOrNullIfZero(p2)
        else -> throw AssertionError()
    }
}

fun TextView.bindErrorMessageAndIconTo(errorProperty: Property<Pair<CharSequence, Drawable>?>) =
        bindViewTo(errorProperty, BindErrorMessageAndIconTo)

@JvmName("bindErrorMessageResAndIconTo")
fun TextView.bindErrorMessageAndIconTo(errorResProperty: Property<MessageAndIconRes?>) =
        bindViewTo(errorResProperty, BindErrorMessageAndIconTo)

private object BindErrorMessageAndIconTo : (TextView, Any?) -> Unit {

    override fun invoke(v: TextView, new: Any?) = when {
        new == null -> v.setError(null, null)
        new is Pair<*, *> -> v.setErrorWithIntrinsicBounds(new.first as CharSequence, new.second as Drawable)
        new is MessageAndIconRes -> {
            val res = v.resources
            v.setErrorWithIntrinsicBounds(res.getText(new.messageRes), res.getDrawableCompat(new.iconRes))
        }
        else -> throw AssertionError()
    }

}

// todo: compound drawables
