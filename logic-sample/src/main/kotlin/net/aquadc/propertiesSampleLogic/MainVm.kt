package net.aquadc.propertiesSampleLogic

import net.aquadc.properties.*
import net.aquadc.properties.function.areNotEqual
import net.aquadc.properties.persistence.PropertyIo
import net.aquadc.properties.persistence.memento.PersistableProperties
import net.aquadc.properties.persistence.x
import java.util.concurrent.TimeUnit

/**
 * This ViewModel can be used both in Android and on JVM.
 * In android-sample it is used in Android Activity,
 * and in fx-sample it is in JavaFX view.
 */
class MainVm(
        private val userProp: MutableProperty<User>
) : PersistableProperties {

    // user input

    val emailProp = propertyOf(userProp.value.email)
    val nameProp = propertyOf(userProp.value.name)
    val surnameProp = propertyOf(userProp.value.surname)
    val buttonClickedProp = propertyOf(false).also {
        it.clearEachAnd { // perform action
            userProp.value = editedUserProp.value
        }
    }

    override fun saveOrRestore(d: PropertyIo) {
        d x emailProp
        d x nameProp
        d x surnameProp
    }

    // a feedback for user actions

    val emailValidProp = emailProp.map { it.contains("@") }

    private val editedUserProp = listOf(emailProp, nameProp, surnameProp).mapValueList { (email, name, surname) ->
        User(email, name, surname)
    }

    private val usersDifferProp = userProp.mapWith(editedUserProp, areNotEqual())

    val buttonEnabledProp = usersDifferProp and emailValidProp
    val debouncedEmail = emailProp.debounced(500, TimeUnit.MILLISECONDS).map { "Debounced e-mail: $it" }

}
