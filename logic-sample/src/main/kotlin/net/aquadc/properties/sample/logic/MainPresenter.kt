package net.aquadc.properties.sample.logic

import net.aquadc.properties.*

class MainPresenter(
        private val userProp: MutableProperty<InMemoryUser>
) {

    class Ui {
        // todo: find a shorter way to create single-threaded properties
        val emailProp = unsynchronizedMutablePropertyOf("")
        val nameProp = unsynchronizedMutablePropertyOf("")
        val surnameProp = unsynchronizedMutablePropertyOf("")

        val emailValidProp = unsynchronizedMutablePropertyOf(false)
        val buttonEnabledProp = unsynchronizedMutablePropertyOf(false)
        val buttonTextProp = unsynchronizedMutablePropertyOf("")
    }

    val ui = Ui()

    private val editedUser = OnScreenUser(
            emailProp = ui.emailProp,
            nameProp = ui.nameProp,
            surnameProp = ui.surnameProp)

    init {
        val usersEqualProp = listOf(userProp, ui.emailProp, ui.nameProp, ui.surnameProp)
                .mapValueList { _ -> userProp.value.equals(editedUser) }

        val currentUser = userProp.value
        ui.emailProp.value = currentUser.email
        ui.nameProp.value = currentUser.name
        ui.surnameProp.value = currentUser.surname

        ui.emailValidProp.bindTo(ui.emailProp.map { it.contains("@") })
        ui.buttonEnabledProp.bindTo(usersEqualProp.mapWith(ui.emailValidProp) { equal, valid -> !equal && valid })
        ui.buttonTextProp.bindTo(usersEqualProp.map { if (it) "Nothing changed" else "Save changes" })
    }

    fun saveButtonClicked() {
        userProp.value = editedUser.snapshot()
    }

}
