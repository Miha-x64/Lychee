package net.aquadc.properties.android

import android.graphics.drawable.Drawable
import android.text.Editable
import android.widget.TextView
import net.aquadc.properties.MutableProperty
import net.aquadc.properties.Property
import net.aquadc.properties.android.compat.getDrawableCompat
import net.aquadc.properties.android.container.MessageAndIconRes
import net.aquadc.properties.android.extension.getTextOrNullIfZero
import net.aquadc.properties.android.extension.setErrorWithIntrinsicBounds
import net.aquadc.properties.android.simple.SimpleTextWatcher

fun TextView.bindTextTo(textProperty: Property<CharSequence>) {
    text = textProperty.value
    textProperty.addChangeListener { _, new -> text = new }
}

@JvmName("bindTextResTo")
fun TextView.bindTextTo(textResProperty: Property<Int>) { // todo: test
    setText(textResProperty.value)
    textResProperty.addChangeListener { _, new -> setText(new) }
}

fun TextView.bindToText(textProperty: MutableProperty<String>) {
    textProperty.value = text.toString()
    addTextChangedListener(object : SimpleTextWatcher() {
        override fun afterTextChanged(s: Editable) {
            textProperty.value = s.toString()
        }
    })
}

fun TextView.bindTextBidirectionally(textProperty: MutableProperty<String>) {
    text = textProperty.value
    var mutatingFromWatcher = false
    var mutatingFromChangeListener = false
    addTextChangedListener(object : SimpleTextWatcher() {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            mutatingFromWatcher = true
        }
        override fun afterTextChanged(s: Editable) {
            if (!mutatingFromChangeListener) textProperty.value = s.toString()
            mutatingFromWatcher = false
        }
    })
    textProperty.addChangeListener { _, new ->
        mutatingFromChangeListener = true
        if (!mutatingFromWatcher) text = new
        mutatingFromChangeListener = false
    }
}


fun TextView.bindHintTo(hintProperty: Property<CharSequence>) { // todo: test
    hint = hintProperty.value
    hintProperty.addChangeListener { _, new -> hint = new }
}

@JvmName("bindHintResTo")
fun TextView.bindHintTo(hintResProperty: Property<Int>) { // todo: test
    setHint(hintResProperty.value)
    hintResProperty.addChangeListener { _, new -> setHint(new) }
}


fun TextView.bindErrorMessageTo(errorProperty: Property<CharSequence?>) {
    error = errorProperty.value
    errorProperty.addChangeListener { _, new -> error = new }
}

@JvmName("bindErrorMessageResTo")
fun TextView.bindErrorMessageTo(errorResProperty: Property<Int>) {
    error = context.resources.getTextOrNullIfZero(errorResProperty.value)
    errorResProperty.addChangeListener { _, new -> error = context.resources.getTextOrNullIfZero(new) }
}

fun TextView.bindErrorMessageAndIconTo(errorProperty: Property<Pair<CharSequence, Drawable>?>) {
    val value = errorProperty.value
    if (value == null) {
        setError(null, null)
    } else {
        setErrorWithIntrinsicBounds(value.first, value.second)
    }
    errorProperty.addChangeListener { _, new ->
        if (new == null) {
            setError(null, null)
        } else {
            val (text, icon) = new
            setErrorWithIntrinsicBounds(text, icon)
        }
    }
}

@JvmName("bindErrorMessageResAndIconTo")
fun TextView.bindErrorMessageAndIconTo(errorResProperty: Property<MessageAndIconRes?>) {

    resources.let { res ->
        val value = errorResProperty.value
        if (value == null) {
            setError(null, null)
        } else {
            setErrorWithIntrinsicBounds(res.getText(value.messageRes), res.getDrawableCompat(value.iconRes))
        }
    }

    errorResProperty.addChangeListener { _, new ->
        resources.let { res ->
            if (new == null) {
                setError(null, null)
            } else {
                setErrorWithIntrinsicBounds(res.getText(new.messageRes), res.getDrawableCompat(new.iconRes))
            }
        }
    }
}

// todo: compound drawables
