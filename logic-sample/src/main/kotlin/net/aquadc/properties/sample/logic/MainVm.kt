package net.aquadc.properties.sample.logic

import net.aquadc.properties.*
import net.aquadc.properties.persistence.PersistableProperties
import net.aquadc.properties.persistence.PropertyIo
import net.aquadc.properties.persistence.x
import java.util.concurrent.TimeUnit

/**
 * This ViewModel can be used both in Android and on JVM.
 * In android-sample it is used in Android Activity,
 * and in fx-sample it is in JavaFX view.
 */
class MainVm(
        private val userProp: MutableProperty<InMemoryUser>
) : PersistableProperties {

    // user input

    val emailProp = propertyOf(userProp.value.email)
    val nameProp = propertyOf(userProp.value.name)
    val surnameProp = propertyOf(userProp.value.surname)
    val buttonClickedProp = propertyOf(false).also {
        it.clearEachAnd { // perform action
            userProp.value = editedUser.snapshot()
        }
    }

    override fun saveOrRestore(d: PropertyIo) {
        d x emailProp
        d x nameProp
        d x surnameProp
    }

    // a feedback for user actions

    val emailValidProp = emailProp.map { it.contains("@") }

    private val editedUser = OnScreenUser(
            emailProp = emailProp,
            nameProp = nameProp,
            surnameProp = surnameProp
    )

    private val usersEqualProp = listOf(userProp, emailProp, nameProp, surnameProp)
            .mapValueList { _ -> userProp.value.equals(editedUser) }

    val buttonEnabledProp = usersEqualProp.mapWith(emailValidProp) { equal, valid -> !equal && valid }
    val buttonTextProp = usersEqualProp.map { if (it) "Nothing changed" else "Save changes" }
    val debouncedEmail = emailProp.debounced(500, TimeUnit.MILLISECONDS).map { "Debounced e-mail: $it" }

}
