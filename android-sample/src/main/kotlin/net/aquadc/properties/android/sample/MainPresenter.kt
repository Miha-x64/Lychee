package net.aquadc.properties.android.sample

import net.aquadc.properties.*

class MainPresenter(
        private val userProp: MutableProperty<InMemoryUser>
) {

    class Ui {
        // todo: find a shorter way to create single-threaded properties
        val emailProp = unsynchronizedMutablePropertyOf("")
        val nameProp = unsynchronizedMutablePropertyOf("")
        val surnameProp = unsynchronizedMutablePropertyOf("")

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

        ui.buttonEnabledProp.bindTo(!usersEqualProp)
        ui.buttonTextProp.bindTo(usersEqualProp.map { if (it) "Nothing changed" else "Save changes" })
    }

    fun viewCreated() {
        val currentUser = userProp.value
        ui.emailProp.value = currentUser.email
        ui.nameProp.value = currentUser.name
        ui.surnameProp.value = currentUser.surname
    }

    fun saveButtonClicked() {
        userProp.value = editedUser.snapshot()
    }

}
