package net.aquadc.properties.fx

import javafx.beans.binding.Bindings
import javafx.beans.binding.BooleanBinding
import javafx.beans.binding.StringBinding
import javafx.beans.binding.When
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import net.aquadc.properties.sample.logic.InMemoryUser
import net.aquadc.properties.sample.logic.User
import java.util.concurrent.Callable

class Presenter(
        private val userProp: SimpleObjectProperty<InMemoryUser>
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

    val buttonEnabledProp: BooleanBinding
    val buttonTextProp: StringBinding

    init {
        val onScreenUser = object : User {
            override val email: String get() = emailProp.get()
            override val name: String get() = nameProp.get()
            override val surname: String get() = surnameProp.get()
        }

        val onScreenUserProp =
                Bindings.createObjectBinding(Callable<User> { onScreenUser }, emailProp, nameProp, surnameProp)

        buttonEnabledProp = !userProp.isEqualTo(onScreenUserProp)
        buttonTextProp = When(buttonEnabledProp).then("Save changes").otherwise("Nothing changed")
    }

    fun saveButtonClicked() {
        userProp.set(InMemoryUser(
                email = emailProp.get(),
                name = nameProp.get(),
                surname = surnameProp.get()
        ))
    }

}
