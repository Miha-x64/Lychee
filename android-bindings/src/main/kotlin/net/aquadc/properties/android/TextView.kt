package net.aquadc.properties.android

import android.text.Editable
import android.text.TextWatcher
import android.widget.TextView
import net.aquadc.properties.MutableProperty
import net.aquadc.properties.mutablePropertyOf

fun TextView.textProperty(): MutableProperty<String> {
    val prop = mutablePropertyOf(text.toString())
    var mutatingFromWatcher = false
    var mutatingFromChangeListener = false
    addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            mutatingFromWatcher = true
        }
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        override fun afterTextChanged(s: Editable) {
            if (!mutatingFromChangeListener) prop.value = s.toString()
            mutatingFromWatcher = false
        }
    })
    prop.addChangeListener { _, new ->
        mutatingFromChangeListener = true
        if (!mutatingFromWatcher) text = new
        mutatingFromChangeListener = false
    }
    return prop
}
