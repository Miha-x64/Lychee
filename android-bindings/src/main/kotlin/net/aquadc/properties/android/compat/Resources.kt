package net.aquadc.properties.android.compat

import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Build
import android.support.annotation.DrawableRes

@Suppress("DEPRECATION")
fun Resources.getDrawableCompat(@DrawableRes id: Int): Drawable =
        if (Build.VERSION.SDK_INT >= 21) getDrawable(id, null) else getDrawable(id)
