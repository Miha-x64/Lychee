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
        bindViewTo(textProperty, ::setText)

@JvmName("bindTextResTo")
fun TextView.bindTextTo(textResProperty: Property<Int>) =
        bindViewTo(textResProperty, ::setText)

fun TextView.bindToText(textProperty: MutableProperty<CharSequence>) {
    textProperty.setValue(text.toString())
    addTextChangedListener(object : SimpleTextWatcher() {
        override fun afterTextChanged(s: Editable) {
            textProperty.setValue(SpannedString(s))
        }
    })
}

fun TextView.bindTextBidirectionally(textProperty: MutableProperty<String>) {
    var mutatingFromWatcher = false
    var mutatingFromChangeListener = false
    addTextChangedListener(object : SimpleTextWatcher() {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            mutatingFromWatcher = true
        }
        override fun afterTextChanged(s: Editable) {
            if (!mutatingFromChangeListener) textProperty.setValue(s.toString())
            mutatingFromWatcher = false
        }
    })
    bindViewTo(textProperty) { new ->
        mutatingFromChangeListener = true
        if (!mutatingFromWatcher) text = new
        mutatingFromChangeListener = false
    }
}


fun TextView.bindHintTo(hintProperty: Property<CharSequence>) =
        bindViewTo(hintProperty, ::setHint)

@JvmName("bindHintResTo")
fun TextView.bindHintTo(hintResProperty: Property<Int>) =
        bindViewTo(hintResProperty, ::setHint)


fun TextView.bindErrorMessageTo(errorProperty: Property<CharSequence?>) =
        bindViewTo(errorProperty, ::setError)

@JvmName("bindErrorMessageResTo")
fun TextView.bindErrorMessageTo(errorResProperty: Property<Int>) =
        bindViewTo(errorResProperty) { new -> error = context.resources.getTextOrNullIfZero(new) }

fun TextView.bindErrorMessageAndIconTo(errorProperty: Property<Pair<CharSequence, Drawable>?>) =
        bindViewTo(errorProperty) { new ->
            if (new == null) {
                setError(null, null)
            } else {
                val (text, icon) = new
                setErrorWithIntrinsicBounds(text, icon)
            }
        }

@JvmName("bindErrorMessageResAndIconTo")
fun TextView.bindErrorMessageAndIconTo(errorResProperty: Property<MessageAndIconRes?>) =
        bindViewTo(errorResProperty) { new ->
            resources.let { res ->
                if (new == null) {
                    setError(null, null)
                } else {
                    setErrorWithIntrinsicBounds(res.getText(new.messageRes), res.getDrawableCompat(new.iconRes))
                }
            }
        }


// todo: compound drawables
