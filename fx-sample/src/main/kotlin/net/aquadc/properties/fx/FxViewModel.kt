package net.aquadc.properties.fx

import javafx.beans.binding.Bindings
import javafx.beans.binding.StringBinding
import javafx.beans.binding.When
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import net.aquadc.propertiesSampleLogic.User
import java.util.concurrent.Callable

class FxViewModel(
        private val userProp: SimpleObjectProperty<User>
) {

    val emailProp: SimpleStringProperty
    val nameProp: SimpleStringProperty
    val surnameProp: SimpleStringProperty

    init {
        val currentUser = userProp.get()
        emailProp = SimpleStringProperty(currentUser.email)
        nameProp = SimpleStringProperty(currentUser.name)
        surnameProp = SimpleStringProperty(currentUser.surname)
    }

    val onScreenUserProp =
            Bindings.createObjectBinding(Callable<User> {
                User(emailProp.value, nameProp.value, surnameProp.value)
            }, emailProp, nameProp, surnameProp)

    val buttonEnabledProp = !userProp.isEqualTo(onScreenUserProp)
    val buttonTextProp: StringBinding = When(buttonEnabledProp).then("Save changes").otherwise("Nothing changed")


    fun saveButtonClicked() {
        userProp.set(onScreenUserProp.value)
    }

}
