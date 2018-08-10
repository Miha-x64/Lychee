package net.aquadc.propertiesSampleApp

import android.app.Application
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.squareup.leakcanary.LeakCanary
import net.aquadc.properties.MutableProperty
import net.aquadc.properties.android.pref.PrefAdapter
import net.aquadc.properties.android.pref.SharedPreferenceProperty
import net.aquadc.propertiesSampleLogic.User
import net.aquadc.propertiesSampleLogic.defaultUser


class App : Application() {

    lateinit var userProp: MutableProperty<User>

    override fun onCreate() {
        super.onCreate()

        if (LeakCanary.isInAnalyzerProcess(this))
            return

        LeakCanary.install(this)

        userProp = SharedPreferenceProperty(
                PreferenceManager.getDefaultSharedPreferences(this), "user", defaultUser,
                object : PrefAdapter<User> {
                    override fun read(prefs: SharedPreferences, key: String, default: User) =
                            User(
                                    email = prefs.getString("${key}_email", default.email),
                                    name = prefs.getString("${key}_name", default.name),
                                    surname = prefs.getString("${key}_surname", default.surname)
                            )
                    override fun save(editor: SharedPreferences.Editor, key: String, value: User) {
                        editor
                                .putString("${key}_email", value.email)
                                .putString("${key}_name", value.name)
                                .putString("${key}_surname", value.surname)
                    }
                    override fun isKeyFor(propKey: String, prefKey: String): Boolean =
                            prefKey == "${propKey}_email" ||
                                    prefKey == "${propKey}_name" ||
                                    prefKey == "${propKey}_surname"
                }
        )
    }

}
