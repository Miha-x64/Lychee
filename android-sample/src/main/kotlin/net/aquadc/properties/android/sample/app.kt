package net.aquadc.properties.android.sample

import android.app.Activity

inline val Activity.app get() = application as App
