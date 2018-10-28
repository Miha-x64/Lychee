package net.aquadc.propertiesSampleApp

import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import net.aquadc.persistence.struct.transaction
import net.aquadc.properties.android.simple.SimpleTextWatcher
import net.aquadc.propertiesSampleLogic.User
import org.jetbrains.anko.*

class MonolithicActivity : Activity() {

    private lateinit var emailInput: EditText
    private lateinit var nameInput: EditText
    private lateinit var surnameInput: EditText
    private lateinit var saveButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val user = app.user

        verticalLayout {
            padding = dip(16)

            emailInput = editText(user[User.Email]) {
                id = 1
                hint = "Email"
            }

            nameInput = editText(user[User.Name]) {
                id = 2
                hint = "Name"
            }

            surnameInput = editText(user[User.Surname]) {
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
        val usersEqual = gatherUser() == app.user.let { listOf(it[User.Email], it[User.Name], it[User.Surname]) }

        saveButton.isEnabled = !usersEqual
        saveButton.text = if (usersEqual) "Nothing changed" else "Save changes"
    }

    private fun saveButtonClicked() {
        app.user.transaction {
            it[User.Email] = emailInput.text.toString()
            it[User.Name] = nameInput.text.toString()
            it[User.Surname] = surnameInput.text.toString()
        }
        textChanged()
    }

    private fun gatherUser() =
            listOf(emailInput.text.toString(), nameInput.text.toString(), surnameInput.text.toString())

}

@Suppress("NOTHING_TO_INLINE")
private inline operator fun TextView.plusAssign(watcher: TextWatcher) = addTextChangedListener(watcher)
