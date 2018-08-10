package net.aquadc.propertiesSampleApp

import android.app.Activity

inline val Activity.app get() = application as App
