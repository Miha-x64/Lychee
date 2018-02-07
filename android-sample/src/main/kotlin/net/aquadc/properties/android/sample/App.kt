package net.aquadc.properties.android.sample

import android.app.Application
import com.squareup.leakcanary.LeakCanary
import net.aquadc.properties.MutableProperty
import net.aquadc.properties.concurrentMutablePropertyOf
import net.aquadc.properties.sample.logic.InMemoryUser
import net.aquadc.properties.sample.logic.defaultUser

class App : Application() {

    val userProp: MutableProperty<InMemoryUser> = concurrentMutablePropertyOf(defaultUser)

    override fun onCreate() {
        super.onCreate()

        if (LeakCanary.isInAnalyzerProcess(this))
            return

        LeakCanary.install(this)
    }

}
