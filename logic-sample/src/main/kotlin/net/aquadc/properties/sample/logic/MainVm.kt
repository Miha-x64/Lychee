package net.aquadc.properties.sample.logic

import net.aquadc.properties.*

class MainVm(
        private val userProp: MutableProperty<InMemoryUser>
) {

    // todo: find a shorter way to create single-threaded properties
    val emailProp = unsynchronizedMutablePropertyOf(userProp.value.email)
    val nameProp = unsynchronizedMutablePropertyOf(userProp.value.name)
    val surnameProp = unsynchronizedMutablePropertyOf(userProp.value.surname)
    val buttonClickedProp = unsynchronizedMutablePropertyOf(false)

    val emailValidProp = unsynchronizedMutablePropertyOf(false)
    val buttonEnabledProp = unsynchronizedMutablePropertyOf(false)
    val buttonTextProp = unsynchronizedMutablePropertyOf("")

    private val editedUser = OnScreenUser(
            emailProp = emailProp,
            nameProp = nameProp,
            surnameProp = surnameProp
    )

    init {
        val usersEqualProp = listOf(userProp, emailProp, nameProp, surnameProp)
                .mapValueList { _ -> userProp.value.equals(editedUser) }

        emailValidProp.bindTo(emailProp.map { it.contains("@") })
        buttonEnabledProp.bindTo(usersEqualProp.mapWith(emailValidProp) { equal, valid -> !equal && valid })
        buttonTextProp.bindTo(usersEqualProp.map { if (it) "Nothing changed" else "Save changes" })
    }

    fun saveButtonClicked() {
        userProp.value = editedUser.snapshot()
    }

}
