package net.aquadc.properties.android.simple

import android.text.Editable
import android.text.TextWatcher

/**
 * Simple adapter class for [TextWatcher].
 */
open class SimpleTextWatcher : TextWatcher {
    /** No-op. */ override fun afterTextChanged(s: Editable) {}
    /** No-op. */ override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
    /** No-op. */ override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
}
