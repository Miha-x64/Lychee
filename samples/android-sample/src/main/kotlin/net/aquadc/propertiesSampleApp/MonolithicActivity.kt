package net.aquadc.propertiesSampleApp

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import net.aquadc.persistence.struct.transaction
import net.aquadc.propertiesSampleLogic.User
import splitties.dimensions.dip
import splitties.views.dsl.core.button
import splitties.views.dsl.core.editText
import splitties.views.dsl.core.verticalLayout
import splitties.views.padding

class MonolithicActivity : Activity() {

    // Oops! You have to store references to views.
    // And make them nullable/lateinit vars.
    // And null them out in Fragments!
    private lateinit var emailInput: EditText
    private lateinit var nameInput: EditText
    private lateinit var surnameInput: EditText
    private lateinit var saveButton: Button

    @SuppressLint("ResourceType")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val user = app.user

        setContentView(verticalLayout {
            padding = dip(16)

            addView(editText {
                id = 1
                setText(user[User.Email])
                hint = "Email"
                emailInput = this // Oops! Assignments are not expressions
            })

            addView(editText {
                id = 2
                setText(user[User.Name])
                hint = "Name"
                nameInput = this
            })

            addView(editText {
                id = 3
                setText(user[User.Surname])
                hint = "Surname"
                surnameInput = this
            })

            addView(button {
                setOnClickListener {
                    saveButtonClicked()
                }
                saveButton = this
            })

        })

        val watcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable) = textChanged()
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
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
            it[Email] = emailInput.text.toString()
            it[Name] = nameInput.text.toString()
            it[Surname] = surnameInput.text.toString()
        }
        textChanged()
    }

    private fun gatherUser() =
            listOf(emailInput.text.toString(), nameInput.text.toString(), surnameInput.text.toString())

    @Suppress("NOTHING_TO_INLINE")
    private inline operator fun TextView.plusAssign(watcher: TextWatcher) = addTextChangedListener(watcher)

}
