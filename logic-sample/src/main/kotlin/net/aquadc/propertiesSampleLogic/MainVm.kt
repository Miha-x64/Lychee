package net.aquadc.propertiesSampleLogic

import net.aquadc.persistence.struct.transaction
import net.aquadc.properties.*
import net.aquadc.properties.function.areNotEqual
import net.aquadc.properties.persistence.*
import net.aquadc.properties.persistence.memento.PersistableProperties
import java.util.concurrent.TimeUnit

/**
 * This ViewModel can be used both in Android and on JVM.
 * In android-sample it is used in Android Activity,
 * and in fx-sample it is in JavaFX view.
 */
class MainVm(
        private val user: TransactionalPropertyStruct<User>
) : PersistableProperties {

    // user input

    private val editableUser = ObservableStruct(user, false)

    val emailProp get() = editableUser prop User.Email
    val nameProp get() = editableUser prop User.Name
    val surnameProp get() = editableUser prop User.Surname

    val buttonClickedProp = propertyOf(false).also {
        it.clearEachAnd { // perform action
            user.transaction { t ->
                t[User.Email] = editableUser[User.Email]
                t[User.Name] = editableUser[User.Name]
                t[User.Surname] = editableUser[User.Surname]
                // TODO: special method for patching
            }
        }
    }

    override fun saveOrRestore(d: PropertyIo) {
        d x emailProp
        d x nameProp
        d x surnameProp
    }

    // a feedback for user actions

    val emailValidProp = emailProp.map { it.contains("@") }

    private val usersDifferProp = user.snapshots().mapWith(editableUser.snapshots(), areNotEqual())

    val buttonEnabledProp = usersDifferProp and emailValidProp
    val debouncedEmail = emailProp.debounced(500, TimeUnit.MILLISECONDS).map { "Debounced e-mail: $it" }

}
