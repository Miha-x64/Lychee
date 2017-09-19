package net.aquadc.properties.android.extension

import android.graphics.drawable.Drawable
import android.widget.TextView

fun TextView.setErrorWithIntrinsicBounds(error: CharSequence, icon: Drawable) {
    icon.setBounds(0, 0, icon.intrinsicWidth, icon.intrinsicHeight)
    setError(error, icon)
}
