package net.aquadc.properties.android.extension

import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import net.aquadc.properties.android.compat.getDrawableCompat

fun Resources.getTextOrNullIfZero(@StringRes id: Int): CharSequence? =
        if (id == 0) null else getText(id)

fun Resources.getDrawableOrNullIfZero(@DrawableRes id: Int): Drawable? =
        if (id == 0) null else getDrawableCompat(id)
