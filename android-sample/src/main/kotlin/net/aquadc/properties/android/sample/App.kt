package net.aquadc.properties.android.sample

import android.app.Application
import net.aquadc.properties.MutableProperty
import net.aquadc.properties.mutablePropertyOf

class App : Application() {

    val userProp: MutableProperty<InMemoryUser> = mutablePropertyOf(
            InMemoryUser(
                    email = "john@riseup.net",
                    name = "John",
                    surname = "Smith"
            )
    )

}
