package net.aquadc.propertiesSampleApp

import android.app.Activity
import android.app.Application
import android.preference.PreferenceManager
import net.aquadc.persistence.android.pref.SharedPreferencesStruct
import net.aquadc.propertiesSampleLogic.User


class App : Application() {

    lateinit var user: SharedPreferencesStruct<User>

    override fun onCreate() {
        super.onCreate()

        user = SharedPreferencesStruct(User, PreferenceManager.getDefaultSharedPreferences(this))
    }

}

inline val Activity.app get() = application as App
