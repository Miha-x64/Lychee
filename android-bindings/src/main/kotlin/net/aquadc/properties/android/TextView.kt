package net.aquadc.properties.android

import android.text.Editable
import android.text.TextWatcher
import android.widget.TextView
import net.aquadc.properties.MutableProperty
import net.aquadc.properties.Property

fun TextView.bindTextTo(textProperty: Property<CharSequence>) {
    text = textProperty.value
    textProperty.addChangeListener { _, new -> text = new }
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

open class SimpleTextWatcher : TextWatcher {
    override fun afterTextChanged(s: Editable) = Unit
    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) = Unit
    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) = Unit
}
