package net.aquadc.propertiessampleapp

import android.app.Activity

inline val Activity.app get() = application as App
