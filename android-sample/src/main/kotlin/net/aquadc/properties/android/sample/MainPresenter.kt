package net.aquadc.properties.android.sample

import net.aquadc.properties.MutableProperty
import net.aquadc.properties.mapValueList
import net.aquadc.properties.not

class MainPresenter(
        private val view: MainPresenter.View,
        private val userProp: MutableProperty<InMemoryUser>
) {

    private val editedUser = OnScreenUser(
            emailProp = view.emailProp,
            nameProp = view.nameProp,
            surnameProp = view.surnameProp)

    init {
        val currentUser = userProp.value
        view.emailProp.value = currentUser.email
        view.nameProp.value = currentUser.name
        view.surnameProp.value = currentUser.surname

        val usersEqualProp = listOf(userProp, view.emailProp, view.nameProp, view.surnameProp)
                .mapValueList { _ -> userProp.value.equals(editedUser) }

        view.buttonEnabledProp.bindTo(!usersEqualProp)
        view.buttonTextProp.bindTo(usersEqualProp.map { if (it) "Nothing changed" else "Save changes" })
    }

    fun saveButtonClicked() {
        userProp.value = editedUser.snapshot()
    }

    interface View {
        val emailProp: MutableProperty<String>
        val nameProp: MutableProperty<String>
        val surnameProp: MutableProperty<String>

        val buttonEnabledProp: MutableProperty<Boolean>
        val buttonTextProp: MutableProperty<String>
    }

}
