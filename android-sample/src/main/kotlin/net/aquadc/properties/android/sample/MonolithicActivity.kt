package net.aquadc.properties.android.sample

import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import net.aquadc.properties.android.SimpleTextWatcher
import org.jetbrains.anko.*

class MonolithicActivity : Activity() {

    private lateinit var emailInput: EditText
    private lateinit var nameInput: EditText
    private lateinit var surnameInput: EditText
    private lateinit var saveButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val user = app.userProp.value

        verticalLayout {
            padding = dip(16)

            emailInput = editText(user.email) {
                id = 1
                hint = "Email"
            }

            nameInput = editText(user.name) {
                id = 2
                hint = "Name"
            }

            surnameInput = editText(user.surname) {
                id = 3
                hint = "Surname"
            }

            saveButton = button {
                setOnClickListener {
                    saveButtonClicked()
                }
            }

        }

        val watcher = object : SimpleTextWatcher() {
            override fun afterTextChanged(s: Editable) = textChanged()
        }
        emailInput += watcher
        nameInput += watcher
        surnameInput += watcher
        textChanged()
    }

    private fun textChanged() {
        val onScreen = InMemoryUser(
                email = emailInput.text.toString(),
                name = nameInput.text.toString(),
                surname = surnameInput.text.toString())

        val usersEqual = onScreen == app.userProp.value

        saveButton.isEnabled = !usersEqual
        saveButton.text = if (usersEqual) "Nothing changed" else "Save changes"
    }

    private fun saveButtonClicked() {
        app.userProp.value =
                InMemoryUser(emailInput.text.toString(), nameInput.text.toString(), surnameInput.text.toString())
        textChanged()
    }

}

@Suppress("NOTHING_TO_INLINE")
private inline operator fun TextView.plusAssign(watcher: TextWatcher) = addTextChangedListener(watcher)
