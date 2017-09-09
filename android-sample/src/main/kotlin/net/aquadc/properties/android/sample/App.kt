package net.aquadc.properties.android.sample

import android.app.Application
import net.aquadc.properties.MutableProperty
import net.aquadc.properties.concurrentMutablePropertyOf
import net.aquadc.properties.sample.logic.InMemoryUser
import net.aquadc.properties.sample.logic.defaultUser

class App : Application() {

    val userProp: MutableProperty<InMemoryUser> = concurrentMutablePropertyOf(defaultUser)

}
