package net.aquadc.propertiesSampleApp

import android.app.Application
import android.preference.PreferenceManager
import com.squareup.leakcanary.LeakCanary
import net.aquadc.properties.android.persistence.pref.SharedPreferenceStruct
import net.aquadc.propertiesSampleLogic.User


class App : Application() {

    lateinit var user: SharedPreferenceStruct<User>

    override fun onCreate() {
        super.onCreate()

        if (LeakCanary.isInAnalyzerProcess(this))
            return

        LeakCanary.install(this)

        user = SharedPreferenceStruct(User, PreferenceManager.getDefaultSharedPreferences(this))
    }

}
