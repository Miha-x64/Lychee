# reactive-properties

Lightweight properties (subjects) implementation.

[Presentation](https://speakerdeck.com/gdg_rnd/mikhail-goriunov-advanced-kotlin-patterns-on-android-properties)

Sample:

```kt
val prop = concurrentMutablePropertyOf(1)
val mapped = prop.map { 10 * it }
assertEquals(10, mapped.value)

prop.value = 5
assertEquals(50, mapped.value)


val tru = concurrentMutablePropertyOf(true)
assertEquals(false, (!tru).value)
```

## Sample usage in GUI application

Anko layout for Android:

```kt
verticalLayout {
    padding = dip(16)

    editText {
        hint = "Email"
        bindTextBidirectionally(vm.emailProp)
        bindErrorMessageTo(vm.emailValidProp.map { if (it) null else "E-mail is invalid" })
    }

    editText {
        hint = "Name"
        bindTextBidirectionally(vm.nameProp)
    }

    editText {
        hint = "Surname"
        bindTextBidirectionally(vm.surnameProp)
    }

    button {
        bindEnabledTo(vm.buttonEnabledProp)
        bindTextTo(vm.buttonTextProp)
        setWhenClicked(vm.buttonClickedProp)
    }

    view().lparams(weight = 1f)

    button("Show Monolithic Activity") {
        setOnClickListener { startActivity(intentFor<MonolithicActivity>()) }
    }

}
```

JavaFx layout (using JFoenix):

```kt
children.add(JFXTextField().apply {
    promptText = "Email"
    textProperty().bindBidirectionally(vm.emailProp)
})

children.add(Label().apply {
    text = "E-mail is invalid"
    bindVisibilityHardlyTo(!vm.emailValidProp)
})

children.add(JFXTextField().apply {
    promptText = "Name"
    textProperty().bindBidirectionally(vm.nameProp)
})

children.add(JFXTextField().apply {
    promptText = "Surname"
    textProperty().bindBidirectionally(vm.surnameProp)
})

children.add(JFXButton("Press me, hey, you!").apply {
    disableProperty().bindTo(!vm.buttonEnabledProp)
    textProperty().bindTo(vm.buttonTextProp)
    setOnAction { vm.buttonClickedProp.set() }
})
```

Common ViewModel:

```kt
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

    buttonClickedProp.takeEachAnd { userProp.value = editedUser.snapshot() }
}
```
