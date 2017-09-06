package net.aquadc.properties.android.sample

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import net.aquadc.properties.Property
import net.aquadc.properties.allValues
import net.aquadc.properties.android.enabledProperty
import net.aquadc.properties.android.textProperty
import org.jetbrains.anko.*

class MainActivity : Activity() {

    private lateinit var nameProp: Property<String>
    private lateinit var surnameProp: Property<String>
    private lateinit var submitView: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        verticalLayout {

            padding = dip(16)

            editText {
                id = 1
                hint = "Name"
                nameProp = textProperty()
            }

            editText {
                id = 2
                hint = "Surname"
                surnameProp = textProperty()
            }

            submitView = button {
                enabledProperty().bind(listOf(nameProp, surnameProp).allValues { it.isNotEmpty() })
                textProperty().bind(nameProp.mapWith(surnameProp) { name, surname ->
                    if (name.isEmpty() || surname.isEmpty()) "Fill in the form to register"
                    else "Register as $name $surname"
                })
            }

        }

    }

}
