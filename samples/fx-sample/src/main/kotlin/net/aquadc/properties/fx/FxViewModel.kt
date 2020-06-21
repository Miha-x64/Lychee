package net.aquadc.properties.fx

import javafx.beans.binding.StringBinding
import javafx.beans.binding.When
import javafx.beans.property.Property
import javafx.beans.property.SimpleBooleanProperty
import net.aquadc.persistence.struct.get
import net.aquadc.properties.function.Objectz
import net.aquadc.properties.mapWith
import net.aquadc.properties.persistence.ObservableStruct
import net.aquadc.properties.persistence.snapshots
import net.aquadc.propertiesSampleLogic.User

class FxViewModel(
        private val user: ObservableStruct<User>
) {

    private val editable = ObservableStruct(user, false)

    val emailProp: Property<String> = (editable prop User.Email).fx()
    val nameProp: Property<String> = (editable prop User.Name).fx()
    val surnameProp: Property<String> = (editable prop User.Surname).fx()

    val buttonEnabledProp = SimpleBooleanProperty().also { it.bind(
            user.snapshots().mapWith(user.snapshots(), Objectz.NotEqual).fx()
    ) }

    val buttonTextProp: StringBinding = When(buttonEnabledProp)
            .then("Save changes").otherwise("Nothing changed")


    fun saveButtonClicked() {
        user[User.Name] = editable[User.Name]
        user[User.Surname] = editable[User.Surname]
        user[User.Email] = editable[User.Email]
    }

}
