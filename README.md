# reactive-properties

Lightweight properties implementation.
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
class MainActivity : Activity() {

    private lateinit var presenter: MainPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        presenter = MainPresenter(app.userProp)
        val uiBridge = presenter.ui

        verticalLayout {
            padding = dip(16)

            editText {
                id = 1
                hint = "Email"
                bindTextBidirectionally(uiBridge.emailProp)
            }

            editText {
                id = 2
                hint = "Name"
                bindTextBidirectionally(uiBridge.nameProp)
            }

            editText {
                id = 3
                hint = "Surname"
                bindTextBidirectionally(uiBridge.surnameProp)
            }

            button {
                bindEnabledTo(uiBridge.buttonEnabledProp)
                bindTextTo(uiBridge.buttonTextProp)
                setOnClickListener { presenter.saveButtonClicked() }
            }

            button("Show Monolithic Activity") {
                setOnClickListener { startActivity(intentFor<MonolithicActivity>()) }
            }

        }
    }

}
```

JavaFx layout (using JFoenix):

```kt
fun viewWithOurProps(presenter: MainPresenter) = VBox(10.0).apply {

    padding = Insets(10.0, 10.0, 10.0, 10.0)

    val uiBridge = presenter.ui

    children.add(JFXTextField().apply {
        promptText = "Email"
        textProperty().bindBidirectionally(uiBridge.emailProp)
    })
    children.add(JFXTextField().apply {
        promptText = "Name"
        textProperty().bindBidirectionally(uiBridge.nameProp)
    })
    children.add(JFXTextField().apply {
        promptText = "Surname"
        textProperty().bindBidirectionally(uiBridge.surnameProp)
    })
    children.add(JFXButton("Press me, hey, you!").apply {
        styleClass.add("button-raised")
        disableProperty().bindTo(!uiBridge.buttonEnabledProp)
        textProperty().bindTo(uiBridge.buttonTextProp)
        setOnAction { presenter.saveButtonClicked() }
    })

}
```

Common presenter:

```kt
class MainPresenter(
        private val userProp: MutableProperty<InMemoryUser>
) {

    class Ui {
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

        val currentUser = userProp.value
        ui.emailProp.value = currentUser.email
        ui.nameProp.value = currentUser.name
        ui.surnameProp.value = currentUser.surname

        ui.buttonEnabledProp.bindTo(!usersEqualProp)
        ui.buttonTextProp.bindTo(usersEqualProp.map { if (it) "Nothing changed" else "Save changes" })
    }

    fun saveButtonClicked() {
        userProp.value = editedUser.snapshot()
    }

}
```
